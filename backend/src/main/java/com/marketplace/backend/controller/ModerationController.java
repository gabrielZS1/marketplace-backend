package com.marketplace.backend.controller;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Report;
import com.marketplace.backend.entity.Review;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.ReportTargetType;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.ReportRepository;
import com.marketplace.backend.repository.ReviewRepository;
import com.marketplace.backend.service.AdminAccessService;
import com.marketplace.backend.service.SubscriptionStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Ferramenta interna de moderação (usada pela página /moderacao). Só administrador.
 *
 * Fluxo: o cliente denuncia (ReportController) → o administrador vê aqui as denúncias
 * abertas → descarta (não viola nada) ou remove o conteúdo (apaga a avaliação ou suspende
 * o estabelecimento). Uma decisão fecha todas as denúncias abertas sobre o mesmo conteúdo.
 */
@RestController
@RequestMapping("/api/admin/moderation")
public class ModerationController {

    private static final Logger log = LoggerFactory.getLogger(ModerationController.class);

    static final String OPEN = "OPEN";
    static final String DISMISSED = "DISMISSED";
    static final String ACTIONED = "ACTIONED";
    private static final Set<String> STATUSES = Set.of(OPEN, DISMISSED, ACTIONED);

    private final AdminAccessService adminAccessService;
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final BusinessRepository businessRepository;
    private final SubscriptionStatusService subscriptionStatusService;

    public ModerationController(AdminAccessService adminAccessService,
                                ReportRepository reportRepository,
                                ReviewRepository reviewRepository,
                                BusinessRepository businessRepository,
                                SubscriptionStatusService subscriptionStatusService) {
        this.adminAccessService = adminAccessService;
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.businessRepository = businessRepository;
        this.subscriptionStatusService = subscriptionStatusService;
    }

    public record TargetSummary(
            boolean exists,
            String title,
            String text,
            Integer rating,
            String imageUrl,
            String businessName,
            Boolean suspended
    ) {}

    public record ReportItem(
            UUID id,
            ReportTargetType targetType,
            UUID targetId,
            String reason,
            String details,
            String status,
            OffsetDateTime createdAt,
            String reporterName,
            long openReportsForTarget,
            TargetSummary target
    ) {}

    public record SuspendedBusiness(UUID id, String name, String city, String logoUrl, OffsetDateTime updatedAt) {}

    /** Admin? (a página usa para decidir se mostra a ferramenta ou "sem acesso"). */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        User admin = adminAccessService.requireAdmin();
        return ResponseEntity.ok(Map.of(
                "name", admin.getName(),
                "openReports", reportRepository.countByStatus(OPEN)));
    }

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReportItem>> reports(@RequestParam(defaultValue = OPEN) String status) {
        adminAccessService.requireAdmin();

        String wanted = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(wanted)) {
            throw new RuntimeException("Status inválido.");
        }

        List<Report> reports = reportRepository.findByStatusWithReporter(wanted, PageRequest.of(0, 200));

        // Quantas denúncias abertas existem por conteúdo (ajuda a priorizar).
        Map<UUID, Long> openPerTarget = new HashMap<>();
        if (OPEN.equals(wanted)) {
            for (Report r : reports) openPerTarget.merge(r.getTargetId(), 1L, Long::sum);
        }

        Map<UUID, TargetSummary> cache = new HashMap<>();
        List<ReportItem> items = new ArrayList<>(reports.size());
        for (Report r : reports) {
            TargetSummary target = cache.computeIfAbsent(r.getTargetId(),
                    id -> summarize(r.getTargetType(), id));
            items.add(new ReportItem(
                    r.getId(), r.getTargetType(), r.getTargetId(), r.getReason(), r.getDetails(),
                    r.getStatus(), r.getCreatedAt(), r.getReporter().getName(),
                    openPerTarget.getOrDefault(r.getTargetId(), 0L), target));
        }
        return ResponseEntity.ok(items);
    }

    /** Denúncia improcedente: fecha esta e as outras abertas sobre o mesmo conteúdo. */
    @PostMapping("/reports/{id}/dismiss")
    @Transactional
    public ResponseEntity<Map<String, Object>> dismiss(@PathVariable UUID id) {
        User admin = adminAccessService.requireAdmin();
        Report report = findReport(id);

        int closed = reportRepository.resolveOpenForTarget(report.getTargetType(), report.getTargetId(), DISMISSED);
        log.info("Moderação: {} descartou denúncias sobre {} {} ({} fechadas)",
                admin.getId(), report.getTargetType(), report.getTargetId(), closed);
        return ResponseEntity.ok(Map.of("closed", closed));
    }

    /**
     * Denúncia procedente: apaga a avaliação, ou suspende o estabelecimento (some da busca e
     * da página pública; o dono continua com os dados e pode recorrer por e-mail).
     */
    @PostMapping("/reports/{id}/remove")
    @Transactional
    public ResponseEntity<Map<String, Object>> remove(@PathVariable UUID id) {
        User admin = adminAccessService.requireAdmin();
        Report report = findReport(id);

        if (report.getTargetType() == ReportTargetType.REVIEW) {
            reviewRepository.findById(report.getTargetId()).ifPresent(reviewRepository::delete);
        } else {
            businessRepository.findById(report.getTargetId()).ifPresent(business -> {
                business.setModerationSuspended(true);
                business.setActive(false);
                businessRepository.save(business);
            });
        }

        int closed = reportRepository.resolveOpenForTarget(report.getTargetType(), report.getTargetId(), ACTIONED);
        log.warn("Moderação: {} removeu {} {} ({} denúncias fechadas)",
                admin.getId(), report.getTargetType(), report.getTargetId(), closed);
        return ResponseEntity.ok(Map.of("closed", closed));
    }

    @GetMapping("/businesses/suspended")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SuspendedBusiness>> suspended() {
        adminAccessService.requireAdmin();
        List<SuspendedBusiness> list = businessRepository.findByModerationSuspendedTrueOrderByUpdatedAtDesc()
                .stream()
                .map(b -> new SuspendedBusiness(b.getId(), b.getName(), b.getCity(), b.getLogoUrl(), b.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(list);
    }

    /** Desfaz a suspensão. Volta a aparecer só se a assinatura também estiver em dia. */
    @PostMapping("/businesses/{id}/reinstate")
    @Transactional
    public ResponseEntity<Map<String, Object>> reinstate(@PathVariable UUID id) {
        User admin = adminAccessService.requireAdmin();
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        business.setModerationSuspended(false);
        boolean active = Boolean.TRUE.equals(business.getOnboardingCompleted())
                && !subscriptionStatusService.effectiveStatus(business).isBlocked();
        business.setActive(active);
        businessRepository.save(business);

        log.warn("Moderação: {} reativou o estabelecimento {} (ativo={})", admin.getId(), id, active);
        return ResponseEntity.ok(Map.of("active", active));
    }

    // ------------------------------------------------------------

    private Report findReport(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Denúncia não encontrada"));
    }

    private TargetSummary summarize(ReportTargetType type, UUID targetId) {
        if (type == ReportTargetType.REVIEW) {
            Optional<Review> opt = reviewRepository.findById(targetId);
            if (opt.isEmpty()) {
                return new TargetSummary(false, "Avaliação já removida", null, null, null, null, null);
            }
            Review r = opt.get();
            return new TargetSummary(true,
                    "Avaliação de " + r.getClient().getName(),
                    r.getComment(), r.getRating(), r.getPhotoUrl(),
                    r.getBusiness().getName(), null);
        }

        Optional<Business> opt = businessRepository.findById(targetId);
        if (opt.isEmpty()) {
            return new TargetSummary(false, "Estabelecimento não existe mais", null, null, null, null, null);
        }
        Business b = opt.get();
        String place = b.getCity() == null ? "" : b.getCity() + (b.getState() == null ? "" : "/" + b.getState());
        return new TargetSummary(true, b.getName(), b.getDescription(), null, b.getLogoUrl(),
                place.isBlank() ? null : place, Boolean.TRUE.equals(b.getModerationSuspended()));
    }
}
