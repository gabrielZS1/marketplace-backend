package com.marketplace.backend.dto.onboarding;

import java.util.UUID;

public class OnboardingStatusResponseDTO {
    private UUID businessId;
    private Boolean onboardingCompleted;

    public OnboardingStatusResponseDTO(UUID businessId, Boolean onboardingCompleted) {
        this.businessId = businessId;
        this.onboardingCompleted = onboardingCompleted;
    }

    public UUID getBusinessId() { return businessId; }
    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
}