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
    private List<String> workspacePhotos;
    private List<String> portfolioPhotos;
    private Double rating;
    private Long reviewCount;

    // Novos campos
    private String phone;
    private String instagramUrl;
    private String tiktokUrl;
    private Boolean hasParking;
    private Boolean allowsPets;
    private Boolean hasWifi;

    public BusinessDetailResponseDTO(
            UUID id,
            String name,
            BusinessCategory category,
            String description,
            String address,
            String city,
            String state,
            Double latitude,
            Double longitude,
            List<String> photos,
            List<String> workspacePhotos,
            List<String> portfolioPhotos,
            Double rating,
            Long reviewCount,
            Boolean featured,
            String phone,
            String instagramUrl,
            String tiktokUrl,
            Boolean hasParking,
            Boolean allowsPets,
            Boolean hasWifi) {

        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.address = address;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photos = photos;
        this.workspacePhotos = workspacePhotos;
        this.portfolioPhotos = portfolioPhotos;
        this.rating = rating;
        this.reviewCount = reviewCount;

        this.phone = phone;
        this.instagramUrl = instagramUrl;
        this.tiktokUrl = tiktokUrl;
        this.hasParking = hasParking;
        this.allowsPets = allowsPets;
        this.hasWifi = hasWifi;
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
    public List<String> getWorkspacePhotos() { return workspacePhotos; }
    public List<String> getPortfolioPhotos() { return portfolioPhotos; }
    public Double getRating() { return rating; }
    public Long getReviewCount() { return reviewCount; }

    // Novos getters
    public String getPhone() { return phone; }
    public String getInstagramUrl() { return instagramUrl; }
    public String getTiktokUrl() { return tiktokUrl; }
    public Boolean getHasParking() { return hasParking; }
    public Boolean getAllowsPets() { return allowsPets; }
    public Boolean getHasWifi() { return hasWifi; }
}