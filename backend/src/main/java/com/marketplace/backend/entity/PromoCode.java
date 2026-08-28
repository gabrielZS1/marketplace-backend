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

    /** Dias de teste concedidos ao resgatar este código (ex: 15 ou 30). */
    @Column(name = "free_days", nullable = false)
    private Integer freeDays;

    /** Rótulo do lote (ex: "LANCAMENTO-15"), só pra organização no painel. */
    @Column(length = 60)
    private String label;

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
    public Integer getFreeDays() { return freeDays; }
    public void setFreeDays(Integer freeDays) { this.freeDays = freeDays; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Boolean getRedeemed() { return redeemed; }
    public void setRedeemed(Boolean redeemed) { this.redeemed = redeemed; }
    public UUID getRedeemedByBusinessId() { return redeemedByBusinessId; }
    public void setRedeemedByBusinessId(UUID redeemedByBusinessId) { this.redeemedByBusinessId = redeemedByBusinessId; }
    public OffsetDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(OffsetDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
