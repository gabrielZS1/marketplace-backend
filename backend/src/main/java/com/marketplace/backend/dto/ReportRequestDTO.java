package com.marketplace.backend.dto;

import com.marketplace.backend.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ReportRequestDTO {

    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private UUID targetId;

    /** SPAM | OFFENSIVE | FAKE | INAPPROPRIATE | OTHER */
    @NotBlank
    @Size(max = 30)
    private String reason;

    @Size(max = 500)
    private String details;

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
