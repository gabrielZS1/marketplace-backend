package com.marketplace.backend.dto.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class WorkingHoursRequestDTO {
    @NotEmpty
    @Valid
    private List<WorkingHourEntryDTO> entries;

    public List<WorkingHourEntryDTO> getEntries() { return entries; }
    public void setEntries(List<WorkingHourEntryDTO> entries) { this.entries = entries; }
}