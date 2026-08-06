package com.marketplace.backend.dto.onboarding;

import jakarta.validation.constraints.Min;

public class CompleteOnboardingRequestDTO {
    @Min(0)
    private Integer trialMonths = 1;

    public Integer getTrialMonths() { return trialMonths; }
    public void setTrialMonths(Integer trialMonths) { this.trialMonths = trialMonths; }
}