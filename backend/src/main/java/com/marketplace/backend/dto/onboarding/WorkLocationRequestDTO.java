package com.marketplace.backend.dto.onboarding;

import com.marketplace.backend.enums.WorkLocationType;
import jakarta.validation.constraints.NotNull;

public class WorkLocationRequestDTO {
    @NotNull
    private WorkLocationType workLocationType;

    public WorkLocationType getWorkLocationType() { return workLocationType; }
    public void setWorkLocationType(WorkLocationType workLocationType) { this.workLocationType = workLocationType; }
}