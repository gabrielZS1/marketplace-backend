package com.marketplace.backend.entity;

import com.marketplace.backend.enums.BusinessCategory;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.enums.WorkLocationType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "businesses")
public class Business {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "business_category")
    private BusinessCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;


    private Double latitude;


    private Double longitude;

    @Column(nullable = false)
    private Boolean featured = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "subscription_status", nullable = false, columnDefinition = "subscription_status")
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(length = 20)
    private String phone;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "tiktok_url", length = 255)
    private String tiktokUrl;

    @Column(name = "has_parking", nullable = false)
    private Boolean hasParking = false;

    @Column(name = "allows_pets", nullable = false)
    private Boolean allowsPets = false;

    @Column(name = "has_wifi", nullable = false)
    private Boolean hasWifi = false;

    @Column(name = "team_size", nullable = false)
    private Integer teamSize = 1;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "work_location_type", nullable = false, columnDefinition = "work_location_type")
    private WorkLocationType workLocationType = WorkLocationType.AT_BUSINESS;

    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "external_customer_id", length = 255)
    private String externalCustomerId;

    @Column(name = "plan_price", precision = 10, scale = 2)
    private BigDecimal planPrice;

    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    @Column(name = "subscription_grace_ends_at")
    private OffsetDateTime subscriptionGraceEndsAt;

    @Column(name = "mp_preapproval_id", length = 255)
    private String mpPreapprovalId;

    public UUID getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BusinessCategory getCategory() { return category; }
    public void setCategory(BusinessCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
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
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
    public String getTiktokUrl() { return tiktokUrl; }
    public void setTiktokUrl(String tiktokUrl) { this.tiktokUrl = tiktokUrl; }
    public Boolean getHasParking() { return hasParking; }
    public void setHasParking(Boolean hasParking) { this.hasParking = hasParking; }
    public Boolean getAllowsPets() { return allowsPets; }
    public void setAllowsPets(Boolean allowsPets) { this.allowsPets = allowsPets; }
    public Boolean getHasWifi() { return hasWifi; }
    public void setHasWifi(Boolean hasWifi) { this.hasWifi = hasWifi; }
    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }
    public WorkLocationType getWorkLocationType() { return workLocationType; }
    public void setWorkLocationType(WorkLocationType workLocationType) { this.workLocationType = workLocationType; }
    public OffsetDateTime getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(OffsetDateTime trialEndsAt) { this.trialEndsAt = trialEndsAt; }
    public OffsetDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(OffsetDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    public String getExternalCustomerId() { return externalCustomerId; }
    public void setExternalCustomerId(String externalCustomerId) { this.externalCustomerId = externalCustomerId; }
    public BigDecimal getPlanPrice() { return planPrice; }
    public void setPlanPrice(BigDecimal planPrice) { this.planPrice = planPrice; }
    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
    public OffsetDateTime getSubscriptionGraceEndsAt() { return subscriptionGraceEndsAt; }
    public void setSubscriptionGraceEndsAt(OffsetDateTime subscriptionGraceEndsAt) { this.subscriptionGraceEndsAt = subscriptionGraceEndsAt; }
    public String getMpPreapprovalId() { return mpPreapprovalId; }
    public void setMpPreapprovalId(String mpPreapprovalId) { this.mpPreapprovalId = mpPreapprovalId; }
}