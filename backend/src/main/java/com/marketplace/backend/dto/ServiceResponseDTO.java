// ServiceResponseDTO.java
package com.marketplace.backend.dto;

import com.marketplace.backend.enums.ServiceLocationType;
import java.math.BigDecimal;
import java.util.UUID;

public class ServiceResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private ServiceLocationType locationType;
    private String category;

    public ServiceResponseDTO(UUID id, String name, String description, BigDecimal price, Integer durationMinutes, ServiceLocationType locationType, String category) {
        this.id = id; this.name = name; this.description = description;
        this.price = price; this.durationMinutes = durationMinutes; this.locationType = locationType;
        this.category = category;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public ServiceLocationType getLocationType() { return locationType; }
    public String getCategory() { return category; }
}