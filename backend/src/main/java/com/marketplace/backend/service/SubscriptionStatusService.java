package com.marketplace.backend.service;

import com.marketplace.backend.dto.BusinessMeResponseDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Calcula o status "real" da assinatura no momento da consulta, sem depender
 * do job diário. Assim o app recebe o estado correto imediatamente quando o
 * trial ou o período de tolerância vencem.
 */
@Service
public class SubscriptionStatusService {

    private final BusinessRepository businessRepository;
    private final PricingService pricingService;

    public SubscriptionStatusService(BusinessRepository businessRepository, PricingService pricingService) {
        this.businessRepository = businessRepository;
        this.pricingService = pricingService;
    }

    /** Status efetivo agora (pode divergir do que está salvo se algo acabou de vencer). */
    public SubscriptionStatus effectiveStatus(Business business) {
        OffsetDateTime now = OffsetDateTime.now();
        SubscriptionStatus current = business.getSubscriptionStatus();

        if (current == SubscriptionStatus.TRIAL
                && business.getTrialEndsAt() != null
                && !business.getTrialEndsAt().isAfter(now)
                && business.getMpPreapprovalId() == null) {
            return SubscriptionStatus.EXPIRED;
        }

        if (current == SubscriptionStatus.PAST_DUE
                && business.getSubscriptionGraceEndsAt() != null
                && !business.getSubscriptionGraceEndsAt().isAfter(now)) {
            return SubscriptionStatus.SUSPENDED;
        }

        return current;
    }

    /**
     * Retorna o status efetivo e, se ele divergir do salvo, persiste a mudança
     * (incluindo o flag active). Use em endpoints de leitura do próprio dono.
     */
    public SubscriptionStatus reconcile(Business business) {
        SubscriptionStatus effective = effectiveStatus(business);
        if (effective != business.getSubscriptionStatus()) {
            business.setSubscriptionStatus(effective);
            if (effective.isBlocked()) {
                business.setActive(false);
            }
            businessRepository.save(business);
        }
        return effective;
    }

    /** Motivo do bloqueio, pro app escolher a tela/texto certo. Null se não bloqueado. */
    public String blockReason(SubscriptionStatus status) {
        return switch (status) {
            case EXPIRED -> "TRIAL_EXPIRED";
            case SUSPENDED -> "PAYMENT_FAILED";
            default -> null;
        };
    }

    /** Monta a resposta de status da assinatura pro app (já reconcilia o estado). */
    public BusinessMeResponseDTO toMeResponse(Business business) {
        return toMeResponse(business, "OWNER", null);
    }

    public BusinessMeResponseDTO toMeResponse(Business business, String role, java.util.UUID employeeId) {
        SubscriptionStatus effective = reconcile(business);
        BigDecimal monthlyPrice = business.getTeamSize() != null
                ? pricingService.monthlyPriceFor(business.getTeamSize())
                : null;
        return new BusinessMeResponseDTO(
                business.getId(),
                business.getName(),
                business.getOnboardingCompleted(),
                effective,
                business.getActive(),
                blockReason(effective),
                business.getTrialEndsAt(),
                business.getSubscriptionGraceEndsAt(),
                business.getCurrentPeriodEnd(),
                business.getTeamSize(),
                monthlyPrice,
                role,
                employeeId
        );
    }
}
