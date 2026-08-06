package com.marketplace.backend.controller;

import com.marketplace.backend.dto.GiftCardRequestDTO;
import com.marketplace.backend.dto.GiftCardResponseDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.GiftCard;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.GiftCardRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/gift-cards")
public class GiftCardController {

    private final GiftCardRepository giftCardRepository;
    private final BusinessRepository businessRepository;

    public GiftCardController(GiftCardRepository giftCardRepository, BusinessRepository businessRepository) {
        this.giftCardRepository = giftCardRepository;
        this.businessRepository = businessRepository;
    }

    @GetMapping
    public List<GiftCardResponseDTO> list(@PathVariable UUID businessId) {
        return giftCardRepository.findByBusinessIdAndActiveTrue(businessId).stream()
                .map(g -> new GiftCardResponseDTO(
                        g.getId(), g.getCategory(), g.getTitle(), g.getDescription(),
                        g.getPrice(), g.getBenefits(), g.getValidityLabel()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<GiftCardResponseDTO> create(@PathVariable UUID businessId, @Valid @RequestBody GiftCardRequestDTO request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        checkIsOwner(business);

        GiftCard giftCard = new GiftCard();
        giftCard.setBusiness(business);
        giftCard.setCategory(request.getCategory());
        giftCard.setTitle(request.getTitle());
        giftCard.setDescription(request.getDescription());
        giftCard.setPrice(request.getPrice());
        giftCard.setBenefits(request.getBenefits());
        giftCard.setValidityLabel(request.getValidityLabel());

        GiftCard saved = giftCardRepository.save(giftCard);
        return ResponseEntity.ok(new GiftCardResponseDTO(
                saved.getId(), saved.getCategory(), saved.getTitle(), saved.getDescription(),
                saved.getPrice(), saved.getBenefits(), saved.getValidityLabel()
        ));
    }

    private void checkIsOwner(Business business) {
        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }
    }
}