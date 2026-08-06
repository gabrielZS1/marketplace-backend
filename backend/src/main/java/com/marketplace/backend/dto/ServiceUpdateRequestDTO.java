
package com.marketplace.backend.dto;

import com.marketplace.backend.enums.ServiceLocationType;
import java.math.BigDecimal;

public class ServiceUpdateRequestDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private ServiceLocationType locationType;
    private String category;
    private Boolean popular;

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
    public Boolean getPopular() { return popular; }
    public void setPopular(Boolean popular) { this.popular = popular; }
}