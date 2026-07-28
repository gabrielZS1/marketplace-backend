// BusinessRequestDTO.java
package com.marketplace.backend.dto;

import com.marketplace.backend.enums.BusinessCategory;
import jakarta.validation.constraints.*;

public class BusinessRequestDTO {
    @NotBlank private String name;
    @NotNull private BusinessCategory category;
    private String description;
    @NotBlank private String address;
    @NotBlank private String city;
    @NotBlank @Size(min = 2, max = 2) private String state;
    @NotNull private Double latitude;
    @NotNull private Double longitude;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BusinessCategory getCategory() { return category; }
    public void setCategory(BusinessCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}