package com.marketplace.backend.dto;

import com.marketplace.backend.entity.BusinessInterval;

import java.time.LocalTime;
import java.util.UUID;

public class BusinessIntervalResponseDTO {
    private UUID id;
    private LocalTime startTime;
    private LocalTime endTime;
    private String label;

    public BusinessIntervalResponseDTO(BusinessInterval i) {
        this.id = i.getId();
        this.startTime = i.getStartTime();
        this.endTime = i.getEndTime();
        this.label = i.getLabel();
    }

    public UUID getId() { return id; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getLabel() { return label; }
}
