// ServiceRequestDTO.java
package com.marketplace.backend.dto;

import com.marketplace.backend.enums.ServiceLocationType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ServiceRequestDTO {
    @NotBlank private String name;
    private String description;
    @NotNull @DecimalMin(value = "0.0", inclusive = true) private BigDecimal price;
    @NotNull @Min(1) private Integer durationMinutes;
    @NotNull private ServiceLocationType locationType;
    private String category;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public ServiceLocationType getLocationType() { return locationType; }
    public void setLocationType(ServiceLocationType locationType) { this.locationType = locationType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}