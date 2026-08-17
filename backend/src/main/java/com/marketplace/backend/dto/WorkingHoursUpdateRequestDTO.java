package com.marketplace.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class WorkingHoursUpdateRequestDTO {

    @NotNull
    @Valid
    private List<WorkingHourRequestDTO> workingHours;

    public List<WorkingHourRequestDTO> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(List<WorkingHourRequestDTO> workingHours) {
        this.workingHours = workingHours;
    }
}