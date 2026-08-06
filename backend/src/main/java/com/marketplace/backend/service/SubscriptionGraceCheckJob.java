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
        List<Business> pastDue = businessRepository.findBySubscriptionStatus(SubscriptionStatus.PAST_DUE);

        OffsetDateTime now = OffsetDateTime.now();

        for (Business business : pastDue) {
            if (business.getSubscriptionGraceEndsAt() != null
                    && business.getSubscriptionGraceEndsAt().isBefore(now)) {
                business.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
                business.setActive(false);
                businessRepository.save(business);
            }
        }
    }
}