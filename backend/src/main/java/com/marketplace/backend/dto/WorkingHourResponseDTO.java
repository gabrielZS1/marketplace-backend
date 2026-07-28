// WorkingHourResponseDTO.java
package com.marketplace.backend.dto;

import java.time.LocalTime;
import java.util.UUID;

public class WorkingHourResponseDTO {
    private UUID id;
    private UUID employeeId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public WorkingHourResponseDTO(UUID id, UUID employeeId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.id = id; this.employeeId = employeeId; this.dayOfWeek = dayOfWeek;
        this.startTime = startTime; this.endTime = endTime;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}