package com.marketplace.backend.dto;

import com.marketplace.backend.entity.TimeOff;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TimeOffResponseDTO {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private Boolean allDay;
    private String reason;

    public TimeOffResponseDTO(TimeOff o) {
        this.id = o.getId();
        this.employeeId = o.getEmployee().getId();
        this.employeeName = o.getEmployee().getUser().getName();
        this.startsAt = o.getStartsAt();
        this.endsAt = o.getEndsAt();
        this.allDay = o.getAllDay();
        this.reason = o.getReason();
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public Boolean getAllDay() { return allDay; }
    public String getReason() { return reason; }
}
