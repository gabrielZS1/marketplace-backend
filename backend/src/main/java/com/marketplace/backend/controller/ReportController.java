package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ReportRequestDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Report;
import com.marketplace.backend.entity.Review;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.ReportTargetType;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.ReportRepository;
import com.marketplace.backend.repository.ReviewRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.service.EmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Set;
import java.util.UUID;

/**
 * Denúncia de conteúdo de usuários (avaliações e estabelecimentos). Fica gravada na
 * tabela `reports` e avisa o administrador por e-mail para análise.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private static final Set<String> REASONS = Set.of("SPAM", "OFFENSIVE", "FAKE", "INAPPROPRIATE", "OTHER");

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ReportController(ReportRepository reportRepository,
                            ReviewRepository reviewRepository,
                            BusinessRepository businessRepository,
                            UserRepository userRepository,
                            EmailService emailService) {
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> report(@Valid @RequestBody ReportRequestDTO request) {
        UUID reporterId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String reason = request.getReason().trim().toUpperCase();
        if (!REASONS.contains(reason)) {
            throw new RuntimeException("Motivo da denúncia inválido.");
        }

        String targetLabel;
        if (request.getTargetType() == ReportTargetType.REVIEW) {
            Review review = reviewRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));
            if (review.getClient().getId().equals(reporterId)) {
                throw new RuntimeException("Você não pode denunciar a sua própria avaliação.");
            }
            targetLabel = "avaliação de " + review.getClient().getName();
        } else {
            Business business = businessRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
            if (business.getOwner().getId().equals(reporterId)) {
                throw new RuntimeException("Você não pode denunciar o seu próprio estabelecimento.");
            }
            targetLabel = "estabelecimento " + business.getName();
        }

        // Denunciar de novo o mesmo conteúdo não cria outra linha (e não é erro).
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, request.getTargetType(), request.getTargetId())) {
            return ResponseEntity.noContent().build();
        }

        String details = request.getDetails() == null ? null : request.getDetails().trim();
        if (details != null && details.isEmpty()) details = null;

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(reason);
        report.setDetails(details);
        reportRepository.save(report);

        log.info("Nova denúncia {} de {} sobre {} {} (motivo {})",
                report.getId(), reporterId, request.getTargetType(), request.getTargetId(), reason);

        // O aviso por e-mail nunca pode derrubar a denúncia.
        try {
            emailService.sendAdminNotice(
                    "Nova denúncia no Glowly: " + reason,
                    "<p>Uma nova denúncia foi registrada.</p><ul>"
                            + "<li><b>Alvo:</b> " + HtmlUtils.htmlEscape(targetLabel)
                            + " (" + request.getTargetType() + " " + request.getTargetId() + ")</li>"
                            + "<li><b>Motivo:</b> " + reason + "</li>"
                            + "<li><b>Detalhes:</b> " + (details == null ? "—" : HtmlUtils.htmlEscape(details)) + "</li>"
                            + "<li><b>Denúncia:</b> " + report.getId() + "</li></ul>"
                            + "<p>Analise e tome providências em até 24 horas.</p>");
        } catch (RuntimeException e) {
            log.error("Falha ao avisar o administrador sobre a denúncia {}", report.getId(), e);
        }

        return ResponseEntity.noContent().build();
    }
}
