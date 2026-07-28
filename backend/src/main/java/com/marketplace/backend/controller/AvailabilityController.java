package com.marketplace.backend.controller;

import com.marketplace.backend.dto.TimeSlotDTO;
import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.WorkingHour;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.ServiceRepository;
import com.marketplace.backend.repository.WorkingHourRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@RestController
public class AvailabilityController {

    private static final int SLOT_STEP_MINUTES = 30;
    private static final ZoneOffset OFFSET = ZoneOffset.of("-03:00");

    private final EmployeeRepository employeeRepository;
    private final WorkingHourRepository workingHourRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;

    public AvailabilityController(EmployeeRepository employeeRepository, WorkingHourRepository workingHourRepository,
                                  AppointmentRepository appointmentRepository, ServiceRepository serviceRepository) {
        this.employeeRepository = employeeRepository;
        this.workingHourRepository = workingHourRepository;
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("/api/businesses/{businessId}/availability")
    public List<TimeSlotDTO> getAvailability(
            @PathVariable UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId,
            @RequestParam(required = false) UUID employeeId) {

        com.marketplace.backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));
        int duration = service.getDurationMinutes();

        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(e -> e.getBusiness().getId().equals(businessId) && e.getActive())
                .filter(e -> employeeId == null || e.getId().equals(employeeId))
                .toList();

        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Java: Monday=1..Sunday=7 -> converte para Domingo=0

        Map<Employee, WorkingHour> workingEmployees = new LinkedHashMap<>();
        for (Employee e : employees) {
            workingHourRepository.findByEmployeeId(e.getId()).stream()
                    .filter(wh -> wh.getDayOfWeek() == dayOfWeek)
                    .findFirst()
                    .ifPresent(wh -> workingEmployees.put(e, wh));
        }

        if (workingEmployees.isEmpty()) {
            return List.of();
        }

        SortedSet<LocalTime> candidateTimes = new TreeSet<>();
        for (WorkingHour wh : workingEmployees.values()) {
            LocalTime t = wh.getStartTime();
            while (!t.plusMinutes(duration).isAfter(wh.getEndTime())) {
                candidateTimes.add(t);
                t = t.plusMinutes(SLOT_STEP_MINUTES);
            }
        }

        OffsetDateTime now = OffsetDateTime.now(OFFSET);

        List<TimeSlotDTO> result = new ArrayList<>();
        for (LocalTime time : candidateTimes) {
            OffsetDateTime slotStart = OffsetDateTime.of(date, time, OFFSET);
            OffsetDateTime slotEnd = slotStart.plusMinutes(duration);

            if (slotStart.isBefore(now)) continue;

            Employee freeEmployee = null;
            for (Map.Entry<Employee, WorkingHour> entry : workingEmployees.entrySet()) {
                WorkingHour wh = entry.getValue();
                boolean withinHours = !time.isBefore(wh.getStartTime()) && !time.plusMinutes(duration).isAfter(wh.getEndTime());
                if (!withinHours) continue;

                Employee candidate = entry.getKey();
                List<Appointment> dayAppointments = appointmentRepository.findByEmployeeIdAndStartsAtBetween(
                        candidate.getId(),
                        OffsetDateTime.of(date, LocalTime.MIN, OFFSET),
                        OffsetDateTime.of(date, LocalTime.MAX, OFFSET)
                );

                boolean isFree = dayAppointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED || a.getStatus() == AppointmentStatus.IN_PROGRESS)
                        .noneMatch(a -> a.getStartsAt().isBefore(slotEnd) && a.getEndsAt().isAfter(slotStart));

                if (isFree) {
                    freeEmployee = candidate;
                    break;
                }
            }

            String timeLabel = time.toString();
            if (freeEmployee != null) {
                result.add(new TimeSlotDTO(timeLabel, true, freeEmployee.getId(), freeEmployee.getUser().getName()));
            } else {
                result.add(new TimeSlotDTO(timeLabel, false, null, null));
            }
        }

        return result;
    }
}