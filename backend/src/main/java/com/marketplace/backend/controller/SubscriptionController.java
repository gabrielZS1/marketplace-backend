package com.marketplace.backend.controller;

import com.marketplace.backend.dto.onboarding.CompleteOnboardingResponseDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/subscribe")
public class SubscriptionController {

    private final BusinessRepository businessRepository;
    private final SubscriptionService subscriptionService;

    public SubscriptionController(BusinessRepository businessRepository, SubscriptionService subscriptionService) {
        this.businessRepository = businessRepository;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<CompleteOnboardingResponseDTO> subscribe(@PathVariable UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }

        SubscriptionService.SubscriptionResult result = subscriptionService.createSubscription(business, 0);
        business.setMpPreapprovalId(result.preapprovalId());
        businessRepository.save(business);

        return ResponseEntity.ok(new CompleteOnboardingResponseDTO(
                business.getId(), business.getOnboardingCompleted(), result.paymentUrl()
        ));
    }
}