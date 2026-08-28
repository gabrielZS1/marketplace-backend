package com.marketplace.backend.dto;

import com.marketplace.backend.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class BusinessMeResponseDTO {
    private UUID id;
    private String name;
    private Boolean onboardingCompleted;

    /** Status efetivo agora (já considera trial/tolerância vencidos). */
    private SubscriptionStatus subscriptionStatus;
    /** true = dono tem acesso ao painel. false = precisa renovar (dados preservados). */
    private Boolean active;
    /** null se não bloqueado; "TRIAL_EXPIRED" ou "PAYMENT_FAILED". */
    private String blockReason;

    private OffsetDateTime trialEndsAt;
    private OffsetDateTime graceEndsAt;
    private OffsetDateTime currentPeriodEnd;

    private Integer teamSize;
    /** Preço que será cobrado por mês, de acordo com a quantidade de profissionais. */
    private BigDecimal monthlyPrice;

    /** "OWNER" ou "EMPLOYEE" — quem está logado nesta conta. */
    private String role;
    /** id do funcionário logado (== id do usuário). Null quando é o dono. */
    private UUID employeeId;

    public BusinessMeResponseDTO(UUID id, String name, Boolean onboardingCompleted,
                                 SubscriptionStatus subscriptionStatus, Boolean active, String blockReason,
                                 OffsetDateTime trialEndsAt, OffsetDateTime graceEndsAt, OffsetDateTime currentPeriodEnd,
                                 Integer teamSize, BigDecimal monthlyPrice, String role, UUID employeeId) {
        this.id = id;
        this.name = name;
        this.onboardingCompleted = onboardingCompleted;
        this.subscriptionStatus = subscriptionStatus;
        this.active = active;
        this.blockReason = blockReason;
        this.trialEndsAt = trialEndsAt;
        this.graceEndsAt = graceEndsAt;
        this.currentPeriodEnd = currentPeriodEnd;
        this.teamSize = teamSize;
        this.monthlyPrice = monthlyPrice;
        this.role = role;
        this.employeeId = employeeId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public Boolean getActive() { return active; }
    public String getBlockReason() { return blockReason; }
    public OffsetDateTime getTrialEndsAt() { return trialEndsAt; }
    public OffsetDateTime getGraceEndsAt() { return graceEndsAt; }
    public OffsetDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public Integer getTeamSize() { return teamSize; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public String getRole() { return role; }
    public UUID getEmployeeId() { return employeeId; }
}
