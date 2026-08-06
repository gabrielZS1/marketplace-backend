package com.marketplace.backend.dto.onboarding;

import java.util.UUID;

public class CompleteOnboardingResponseDTO {
    private UUID businessId;
    private Boolean onboardingCompleted;
    private String paymentUrl;

    public CompleteOnboardingResponseDTO(UUID businessId, Boolean onboardingCompleted, String paymentUrl) {
        this.businessId = businessId;
        this.onboardingCompleted = onboardingCompleted;
        this.paymentUrl = paymentUrl;
    }

    public UUID getBusinessId() { return businessId; }
    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public String getPaymentUrl() { return paymentUrl; }
}