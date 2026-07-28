// BusinessResponseDTO.java
package com.marketplace.backend.dto;

import com.marketplace.backend.enums.BusinessCategory;
import com.marketplace.backend.enums.SubscriptionStatus;
import java.util.UUID;

public class BusinessResponseDTO {
    private UUID id;
    private String name;
    private BusinessCategory category;
    private String description;
    private String logoUrl;
    private String address;
    private String city;
    private String state;
    private Double latitude;
    private Double longitude;
    private Double distanceKm; // null quando não é busca por proximidade

    public BusinessResponseDTO(UUID id, String name, BusinessCategory category, String description, String logoUrl,
                               String address, String city, String state, Double latitude, Double longitude, Double distanceKm) {
        this.id = id; this.name = name; this.category = category; this.description = description;
        this.logoUrl = logoUrl; this.address = address; this.city = city; this.state = state;
        this.latitude = latitude; this.longitude = longitude; this.distanceKm = distanceKm;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BusinessCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getLogoUrl() { return logoUrl; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getDistanceKm() { return distanceKm; }
}