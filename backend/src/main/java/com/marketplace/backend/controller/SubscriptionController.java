package com.marketplace.backend.controller;

import com.marketplace.backend.dto.onboarding.CompleteOnboardingResponseDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.service.PricingService;
import com.marketplace.backend.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/subscribe")
public class SubscriptionController {

    private final BusinessRepository businessRepository;
    private final SubscriptionService subscriptionService;
    private final PricingService pricingService;

    public SubscriptionController(BusinessRepository businessRepository,
                                 SubscriptionService subscriptionService,
                                 PricingService pricingService) {
        this.businessRepository = businessRepository;
        this.subscriptionService = subscriptionService;
        this.pricingService = pricingService;
    }

    /**
     * Gera (ou regenera) a assinatura recorrente mensal no Mercado Pago e
     * devolve o link de pagamento. Usado tanto na tela de renovação (trial
     * expirado / assinatura suspensa) quanto pra assinar durante o trial.
     */
    @PostMapping
    public ResponseEntity<CompleteOnboardingResponseDTO> subscribe(@PathVariable UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }

        // Garante que o preço está de acordo com a quantidade de profissionais.
        business.setPlanPrice(pricingService.monthlyPriceFor(business.getTeamSize()));

        // Se ainda está no trial, os dias restantes viram free_trial no Mercado Pago
        // pra o cliente não pagar em dobro.
        int freeTrialDays = 0;
        if (business.getSubscriptionStatus() == SubscriptionStatus.TRIAL
                && business.getTrialEndsAt() != null
                && business.getTrialEndsAt().isAfter(OffsetDateTime.now())) {
            freeTrialDays = (int) ChronoUnit.DAYS.between(OffsetDateTime.now(), business.getTrialEndsAt());
        }

        SubscriptionService.SubscriptionResult result =
                subscriptionService.createSubscription(business, freeTrialDays);
        business.setMpPreapprovalId(result.preapprovalId());
        businessRepository.save(business);

        return ResponseEntity.ok(new CompleteOnboardingResponseDTO(
                business.getId(), business.getOnboardingCompleted(), result.paymentUrl()
        ));
    }
}
