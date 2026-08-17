package com.marketplace.backend.controller;

import com.marketplace.backend.dto.BusinessMeResponseDTO;
import com.marketplace.backend.dto.PushTokenUpdateRequestDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.repository.BusinessRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/me")
public class BusinessAccountController {

    private final BusinessRepository businessRepository;

    public BusinessAccountController(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @GetMapping
    public ResponseEntity<BusinessMeResponseDTO> me() {
        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());

        List<Business> businesses = businessRepository.findAll().stream()
                .filter(b -> b.getOwner().getId().equals(loggedUserId))
                .toList();

        if (businesses.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Business business = businesses.get(0);

        return ResponseEntity.ok(new BusinessMeResponseDTO(
                business.getId(), business.getName(), business.getOnboardingCompleted(),
                business.getSubscriptionStatus(), business.getTrialEndsAt(),
                business.getTeamSize(), business.getPlanPrice(), business.getActive()
        ));
    }

    @PatchMapping("/push-token")
    public ResponseEntity<Void> updatePushToken(
            @Valid @RequestBody PushTokenUpdateRequestDTO request
    ) {
        UUID loggedUserId = UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        Business business = businessRepository.findAll().stream()
                .filter(b -> b.getOwner().getId().equals(loggedUserId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Negócio não encontrado"));

        business.setExpoPushToken(request.getExpoPushToken());
        businessRepository.save(business);

        return ResponseEntity.noContent().build();
    }
}