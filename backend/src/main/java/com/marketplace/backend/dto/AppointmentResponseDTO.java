package com.marketplace.backend.dto;

import com.marketplace.backend.enums.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AppointmentResponseDTO {
    private UUID id;
    private UUID businessId;
    private String businessName;
    private String clientName;
    private String employeeName;
    private String serviceName;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private AppointmentStatus status;
    private Boolean isHomeService;
    private String notes;

    public AppointmentResponseDTO(UUID id, UUID businessId, String businessName, String clientName, String employeeName, String serviceName,
                                  OffsetDateTime startsAt, OffsetDateTime endsAt, AppointmentStatus status, Boolean isHomeService, String notes) {
        this.id = id;
        this.businessId = businessId;
        this.businessName = businessName;
        this.clientName = clientName;
        this.employeeName = employeeName;
        this.serviceName = serviceName;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
        this.isHomeService = isHomeService;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public String getBusinessName() { return businessName; }
    public String getClientName() { return clientName; }
    public String getEmployeeName() { return employeeName; }
    public String getServiceName() { return serviceName; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public AppointmentStatus getStatus() { return status; }
    public Boolean getIsHomeService() { return isHomeService; }
    public String getNotes() { return notes; }
}