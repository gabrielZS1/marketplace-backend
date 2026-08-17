package com.marketplace.backend.dto;

import com.marketplace.backend.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class BusinessMeResponseDTO {
    private UUID id;
    private String name;
    private Boolean onboardingCompleted;
    private SubscriptionStatus subscriptionStatus;
    private OffsetDateTime trialEndsAt;
    private Integer teamSize;
    private BigDecimal planPrice;
    private Boolean active;

    public BusinessMeResponseDTO(UUID id, String name, Boolean onboardingCompleted, SubscriptionStatus subscriptionStatus,
                                 OffsetDateTime trialEndsAt, Integer teamSize, BigDecimal planPrice, Boolean active) {
        this.id = id;
        this.name = name;
        this.onboardingCompleted = onboardingCompleted;
        this.subscriptionStatus = subscriptionStatus;
        this.trialEndsAt = trialEndsAt;
        this.teamSize = teamSize;
        this.planPrice = planPrice;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public OffsetDateTime getTrialEndsAt() { return trialEndsAt; }
    public Integer getTeamSize() { return teamSize; }
    public BigDecimal getPlanPrice() { return planPrice; }
    public Boolean getActive() { return active; }
}