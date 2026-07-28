package com.marketplace.backend.dto;

import java.util.UUID;

public class TimeSlotDTO {
    private String time;
    private Boolean available;
    private UUID employeeId;
    private String employeeName;

    public TimeSlotDTO(String time, Boolean available, UUID employeeId, String employeeName) {
        this.time = time; this.available = available;
        this.employeeId = employeeId; this.employeeName = employeeName;
    }

    public String getTime() { return time; }
    public Boolean getAvailable() { return available; }
    public UUID getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
}