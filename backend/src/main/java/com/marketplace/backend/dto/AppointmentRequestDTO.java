// AppointmentRequestDTO.java
package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AppointmentRequestDTO {
    private UUID employeeId;
    @NotNull private UUID serviceId;
    @NotNull private OffsetDateTime startsAt;
    private Boolean isHomeService = false;
    private String clientAddress;
    private String notes;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public Boolean getIsHomeService() { return isHomeService; }
    public void setIsHomeService(Boolean isHomeService) { this.isHomeService = isHomeService; }
    public String getClientAddress() { return clientAddress; }
    public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}