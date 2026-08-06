package com.marketplace.backend.dto.onboarding;

import com.marketplace.backend.enums.BusinessCategory;
import jakarta.validation.constraints.NotNull;

public class StartOnboardingRequestDTO {
    @NotNull
    private BusinessCategory category;

    public BusinessCategory getCategory() { return category; }
    public void setCategory(BusinessCategory category) { this.category = category; }
}