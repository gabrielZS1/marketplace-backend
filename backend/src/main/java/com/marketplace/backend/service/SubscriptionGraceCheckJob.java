package com.marketplace.backend.service;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class SubscriptionGraceCheckJob {

    private final BusinessRepository businessRepository;

    public SubscriptionGraceCheckJob(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    // Roda todo dia às 3h da manhã
    @Scheduled(cron = "0 0 3 * * *")
    public void checkOverdueSubscriptions() {
        OffsetDateTime now = OffsetDateTime.now();

        // Caso 1: pagamento atrasado (PAST_DUE) que passou do período de tolerância
        List<Business> pastDue = businessRepository.findBySubscriptionStatus(SubscriptionStatus.PAST_DUE);
        for (Business business : pastDue) {
            if (business.getSubscriptionGraceEndsAt() != null
                    && business.getSubscriptionGraceEndsAt().isBefore(now)) {
                business.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
                business.setActive(false);
                businessRepository.save(business);
            }
        }

        // Caso 2: trial de 7 dias (sem assinatura no Mercado Pago) que venceu
        List<Business> onTrial = businessRepository.findBySubscriptionStatus(SubscriptionStatus.TRIAL);
        for (Business business : onTrial) {
            boolean neverSubscribed = business.getMpPreapprovalId() == null;
            boolean trialExpired = business.getTrialEndsAt() != null && business.getTrialEndsAt().isBefore(now);

            if (neverSubscribed && trialExpired) {
                business.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
                business.setActive(false);
                businessRepository.save(business);
            }
        }
    }
}