package com.marketplace.backend.service;

import com.marketplace.backend.dto.BusinessStatsResponseDTO;
import com.marketplace.backend.dto.BusinessStatsResponseDTO.*;
import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.entity.Review;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusinessStatsService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;

    public BusinessStatsService(AppointmentRepository appointmentRepository,
                                ReviewRepository reviewRepository) {
        this.appointmentRepository = appointmentRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public BusinessStatsResponseDTO build(UUID businessId, int periodDays) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = now.minusDays(periodDays);

        List<Appointment> appts = appointmentRepository.findForStats(businessId, from, now.plusYears(1));
        // startsAt no futuro também conta como "upcoming"; o filtro do query pega de `from` em diante.

        List<Appointment> inPeriod = appts.stream()
                .filter(a -> !a.getStartsAt().isAfter(now))
                .toList();

        return new BusinessStatsResponseDTO(
                periodDays,
                from,
                now,
                appointments(inPeriod),
                revenue(appts, now),
                clients(businessId, inPeriod, from, now),
                topServices(inPeriod),
                team(inPeriod),
                reviews(businessId, from),
                busiest(inPeriod)
        );
    }

    // ---------------------------------------------------------------

    private Appointments appointments(List<Appointment> a) {
        long total = a.size();
        long completed = count(a, AppointmentStatus.COMPLETED);
        long confirmed = count(a, AppointmentStatus.CONFIRMED);
        long pending = count(a, AppointmentStatus.PENDING);
        long inProgress = count(a, AppointmentStatus.IN_PROGRESS);
        long cancelled = count(a, AppointmentStatus.CANCELLED);
        long noShow = count(a, AppointmentStatus.NO_SHOW);

        long finished = completed + cancelled + noShow;
        double completionRate = finished == 0 ? 0.0 : round2((double) completed / finished);
        double cancellationRate = total == 0 ? 0.0 : round2((double) (cancelled + noShow) / total);

        return new Appointments(total, completed, confirmed, pending, inProgress, cancelled, noShow,
                completionRate, cancellationRate);
    }

    private Revenue revenue(List<Appointment> all, OffsetDateTime now) {
        BigDecimal realized = BigDecimal.ZERO;
        BigDecimal upcoming = BigDecimal.ZERO;
        BigDecimal lost = BigDecimal.ZERO;
        long completedCount = 0;

        for (Appointment a : all) {
            BigDecimal price = a.getService().getPrice() == null ? BigDecimal.ZERO : a.getService().getPrice();
            switch (a.getStatus()) {
                case COMPLETED -> {
                    realized = realized.add(price);
                    completedCount++;
                }
                case PENDING, CONFIRMED -> {
                    if (a.getStartsAt().isAfter(now)) {
                        upcoming = upcoming.add(price);
                    }
                }
                case CANCELLED, NO_SHOW -> lost = lost.add(price);
                default -> { }
            }
        }

        BigDecimal averageTicket = completedCount == 0
                ? BigDecimal.ZERO
                : realized.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP);

        return new Revenue(realized, upcoming, lost, averageTicket);
    }

    private Clients clients(UUID businessId, List<Appointment> inPeriod,
                            OffsetDateTime from, OffsetDateTime to) {
        Set<UUID> unique = inPeriod.stream()
                .map(a -> a.getClient().getId())
                .collect(Collectors.toSet());

        long newInPeriod = appointmentRepository.findFirstAppointmentPerClient(businessId).stream()
                .filter(row -> {
                    OffsetDateTime first = (OffsetDateTime) row[1];
                    return !first.isBefore(from) && !first.isAfter(to);
                })
                .map(row -> (UUID) row[0])
                .filter(unique::contains)
                .count();

        return new Clients(unique.size(), newInPeriod, Math.max(0, unique.size() - newInPeriod));
    }

    private List<ServiceStat> topServices(List<Appointment> a) {
        Map<String, long[]> counts = new HashMap<>();
        Map<String, BigDecimal> revenue = new HashMap<>();

        for (Appointment ap : a) {
            String name = ap.getService().getName();
            counts.computeIfAbsent(name, k -> new long[1])[0]++;
            if (ap.getStatus() == AppointmentStatus.COMPLETED) {
                BigDecimal price = ap.getService().getPrice() == null ? BigDecimal.ZERO : ap.getService().getPrice();
                revenue.merge(name, price, BigDecimal::add);
            }
        }

        return counts.entrySet().stream()
                .map(e -> new ServiceStat(e.getKey(), e.getValue()[0],
                        revenue.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparingLong(ServiceStat::count).reversed())
                .limit(5)
                .toList();
    }

    private List<EmployeeStat> team(List<Appointment> a) {
        Map<UUID, List<Appointment>> byEmployee = a.stream()
                .collect(Collectors.groupingBy(ap -> ap.getEmployee().getId()));

        return byEmployee.entrySet().stream()
                .map(e -> {
                    List<Appointment> list = e.getValue();
                    Appointment sample = list.get(0);
                    BigDecimal rev = list.stream()
                            .filter(ap -> ap.getStatus() == AppointmentStatus.COMPLETED)
                            .map(ap -> ap.getService().getPrice() == null ? BigDecimal.ZERO : ap.getService().getPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Double rating = reviewRepository.findAverageRatingByEmployeeId(e.getKey());
                    return new EmployeeStat(
                            e.getKey(),
                            sample.getEmployee().getUser().getName(),
                            list.size(),
                            list.stream().filter(ap -> ap.getStatus() == AppointmentStatus.COMPLETED).count(),
                            rev,
                            rating == null ? null : round2(rating)
                    );
                })
                .sorted(Comparator.comparing(EmployeeStat::revenue).reversed())
                .toList();
    }

    private Reviews reviews(UUID businessId, OffsetDateTime from) {
        Double average = reviewRepository.findAverageRatingByBusinessId(businessId);

        List<Review> recent = reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .filter(r -> !r.getCreatedAt().isBefore(from))
                .toList();

        long[] dist = new long[6];
        for (Review r : recent) {
            int stars = Math.max(1, Math.min(5, r.getRating()));
            dist[stars]++;
        }

        return new Reviews(
                average == null ? null : round2(average),
                recent.size(),
                dist[5], dist[4], dist[3], dist[2], dist[1]
        );
    }

    private Busiest busiest(List<Appointment> a) {
        if (a.isEmpty()) {
            return new Busiest(null, null);
        }
        Map<DayOfWeek, Long> byDay = a.stream()
                .collect(Collectors.groupingBy(ap -> ap.getStartsAt().getDayOfWeek(), Collectors.counting()));
        Map<Integer, Long> byHour = a.stream()
                .collect(Collectors.groupingBy(ap -> ap.getStartsAt().getHour(), Collectors.counting()));

        DayOfWeek day = byDay.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        Integer hour = byHour.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        return new Busiest(
                day == null ? null : day.getDisplayName(TextStyle.FULL, PT_BR),
                hour
        );
    }

    // ---------------------------------------------------------------

    private static long count(List<Appointment> a, AppointmentStatus s) {
        return a.stream().filter(ap -> ap.getStatus() == s).count();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
