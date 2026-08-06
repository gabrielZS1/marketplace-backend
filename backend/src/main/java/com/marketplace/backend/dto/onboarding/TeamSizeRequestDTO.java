package com.marketplace.backend.dto.onboarding;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TeamSizeRequestDTO {
    @NotNull
    @Min(1)
    private Integer teamSize;

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }
}