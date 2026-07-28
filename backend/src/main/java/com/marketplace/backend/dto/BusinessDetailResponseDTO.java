package com.marketplace.backend.dto;

import com.marketplace.backend.enums.BusinessCategory;
import java.util.List;
import java.util.UUID;

public class BusinessDetailResponseDTO {
    private UUID id;
    private String name;
    private BusinessCategory category;
    private String description;
    private String address;
    private String city;
    private String state;
    private Double latitude;
    private Double longitude;
    private List<String> photos;
    private Double rating;
    private Long reviewCount;

    public BusinessDetailResponseDTO(UUID id, String name, BusinessCategory category, String description,
                                     String address, String city, String state, Double latitude, Double longitude,
                                     List<String> photos, Double rating, Long reviewCount, Boolean featured) {
        this.id = id; this.name = name; this.category = category; this.description = description;
        this.address = address; this.city = city; this.state = state;
        this.latitude = latitude; this.longitude = longitude; this.photos = photos;
        this.rating = rating; this.reviewCount = reviewCount;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BusinessCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public List<String> getPhotos() { return photos; }
    public Double getRating() { return rating; }
    public Long getReviewCount() { return reviewCount; }
}