package com.marketplace.backend.service;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Avisos de cobrança por e-mail. A assinatura é feita pela página web /assinar (o app das lojas
 * não mostra preço nem botão de compra), então o dono precisa receber o caminho por e-mail:
 *  - 3 dias antes do fim do teste grátis;
 *  - quando o teste acaba ou o pagamento falha e o acesso é bloqueado.
 * Cada aviso sai uma vez por ciclo (campos zerados quando a assinatura volta a ficar ativa).
 */
@Component
public class SubscriptionReminderJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionReminderJob.class);

    private static final Duration REMINDER_WINDOW = Duration.ofDays(3);

    private final BusinessRepository businessRepository;
    private final EmailService emailService;
    private final String baseUrl;

    public SubscriptionReminderJob(BusinessRepository businessRepository,
                                   EmailService emailService,
                                   @Value("${app.base-url}") String baseUrl) {
        this.businessRepository = businessRepository;
        this.emailService = emailService;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    // Todo dia ao meio-dia (UTC) = 9h em Brasília. (O horário pode ser trocado por propriedade.)
    @Scheduled(cron = "${app.billing.reminder-cron:0 0 12 * * *}")
    @Transactional
    public void sendNotices() {
        // Sem e-mail configurado nada sai — e então NÃO marcamos como enviado, para tentar
        // de novo no dia em que o Resend estiver configurado.
        if (!emailService.isConfigured()) {
            log.warn("Avisos de cobrança não enviados: RESEND_API_KEY não configurada.");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        String link = baseUrl + "/assinar";
        int reminders = 0;
        int notices = 0;

        // 1) Teste grátis perto do fim, sem assinatura criada.
        for (Business business : businessRepository.findBySubscriptionStatus(SubscriptionStatus.TRIAL)) {
            if (business.getMpPreapprovalId() != null || business.getTrialReminderSentAt() != null) continue;
            OffsetDateTime ends = business.getTrialEndsAt();
            if (ends == null || !ends.isAfter(now) || ends.isAfter(now.plus(REMINDER_WINDOW))) continue;
            if (!ownerIsActive(business)) continue;

            long days = Math.max(1, ChronoUnit.DAYS.between(now, ends));
            emailService.sendSubscriptionNotice(
                    business.getOwner().getEmail(),
                    "Seu teste grátis do Glowly termina em breve",
                    "Seu teste termina em " + days + (days == 1 ? " dia" : " dias"),
                    "Para continuar usando o Glowly Business com " + business.getName()
                            + ", ative sua assinatura pela página abaixo.",
                    link);
            business.setTrialReminderSentAt(now);
            businessRepository.save(business);
            reminders++;
        }

        // 2) Acesso bloqueado (teste acabou ou pagamento não regularizado).
        List<Business> blocked = new ArrayList<>(businessRepository.findBySubscriptionStatus(SubscriptionStatus.EXPIRED));
        blocked.addAll(businessRepository.findBySubscriptionStatus(SubscriptionStatus.SUSPENDED));
        for (Business business : blocked) {
            if (business.getExpiredNoticeSentAt() != null || !ownerIsActive(business)) continue;

            boolean payment = business.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED;
            emailService.sendSubscriptionNotice(
                    business.getOwner().getEmail(),
                    payment ? "Sua assinatura do Glowly está suspensa" : "Seu teste grátis do Glowly terminou",
                    payment ? "Assinatura suspensa" : "Seu período de teste terminou",
                    "Seus dados continuam salvos. Para voltar a usar o Glowly Business com " + business.getName()
                            + ", regularize sua assinatura pela página abaixo e depois abra o app.",
                    link);
            business.setExpiredNoticeSentAt(now);
            businessRepository.save(business);
            notices++;
        }

        if (reminders > 0 || notices > 0) {
            log.info("Avisos de cobrança enviados: {} lembrete(s) de teste, {} aviso(s) de bloqueio", reminders, notices);
        }
    }

    private boolean ownerIsActive(Business business) {
        return business.getOwner() != null
                && Boolean.TRUE.equals(business.getOwner().getActive())
                && business.getOwner().getEmail() != null;
    }
}
