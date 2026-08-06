package com.marketplace.backend.dto;

import com.marketplace.backend.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public class AppointmentStatusUpdateRequestDTO {
    @NotNull
    private AppointmentStatus status;

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
}