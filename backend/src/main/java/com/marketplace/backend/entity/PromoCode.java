package com.marketplace.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "promo_codes")
public class PromoCode {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "free_months", nullable = false)
    private Integer freeMonths;

    @Column(nullable = false)
    private Boolean redeemed = false;

    @Column(name = "redeemed_by_business_id")
    private UUID redeemedByBusinessId;

    @Column(name = "redeemed_at")
    private OffsetDateTime redeemedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getFreeMonths() { return freeMonths; }
    public void setFreeMonths(Integer freeMonths) { this.freeMonths = freeMonths; }
    public Boolean getRedeemed() { return redeemed; }
    public void setRedeemed(Boolean redeemed) { this.redeemed = redeemed; }
    public UUID getRedeemedByBusinessId() { return redeemedByBusinessId; }
    public void setRedeemedByBusinessId(UUID redeemedByBusinessId) { this.redeemedByBusinessId = redeemedByBusinessId; }
    public OffsetDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(OffsetDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}