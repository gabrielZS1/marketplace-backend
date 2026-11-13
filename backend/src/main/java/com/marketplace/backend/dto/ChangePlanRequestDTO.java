package com.marketplace.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ChangePlanRequestDTO {

    /** 1 a 5 profissionais (5 = "5 ou mais"). */
    @NotNull
    @Min(1)
    @Max(5)
    private Integer teamSize;

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }
}
