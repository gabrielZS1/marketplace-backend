package com.marketplace.backend.controller;

import com.marketplace.backend.dto.BusinessMeResponseDTO;
import com.marketplace.backend.dto.ChangePlanRequestDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.service.PricingService;
import com.marketplace.backend.service.SubscriptionService;
import com.marketplace.backend.service.SubscriptionStatusService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Troca de plano (quantidade de profissionais) — tanto upgrade quanto downgrade.
 *
 * Regras:
 *  - só o dono, e só com acesso liberado (trial / ativa / em tolerância);
 *  - não deixa reduzir abaixo do número de profissionais ativos;
 *  - se já existe assinatura no Mercado Pago, o valor dela é atualizado ANTES de
 *    gravar qualquer coisa aqui (se o MP recusar, nada muda);
 *  - sem assinatura ainda (trial), só o plano/preço locais mudam — o valor novo
 *    é usado quando o dono assinar.
 * O novo valor vale a partir da próxima cobrança; o limite de profissionais muda na hora.
 */
@RestController
@RequestMapping("/api/businesses/{businessId}/plan")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final PricingService pricingService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionStatusService subscriptionStatusService;

    public PlanController(BusinessRepository businessRepository,
                          EmployeeRepository employeeRepository,
                          PricingService pricingService,
                          SubscriptionService subscriptionService,
                          SubscriptionStatusService subscriptionStatusService) {
        this.businessRepository = businessRepository;
        this.employeeRepository = employeeRepository;
        this.pricingService = pricingService;
        this.subscriptionService = subscriptionService;
        this.subscriptionStatusService = subscriptionStatusService;
    }

    @PatchMapping
    public ResponseEntity<BusinessMeResponseDTO> changePlan(
            @PathVariable UUID businessId,
            @Valid @RequestBody ChangePlanRequestDTO request
    ) {
        // Evita duas trocas simultâneas do mesmo negócio (toque duplo / dois aparelhos).
        synchronized (lockFor(businessId)) {
            return ResponseEntity.ok(doChangePlan(businessId, request.getTeamSize()));
        }
    }

    private BusinessMeResponseDTO doChangePlan(UUID businessId, int target) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }

        SubscriptionStatus effective = subscriptionStatusService.reconcile(business);
        if (effective.isBlocked()) {
            throw new RuntimeException("Regularize sua assinatura antes de trocar de plano.");
        }

        int current = business.getTeamSize() != null ? business.getTeamSize() : 1;
        if (target == current) {
            throw new RuntimeException("Você já está nesse plano.");
        }

        int activeProfessionals = employeeRepository.findByBusinessIdAndActiveTrue(businessId).size();
        if (target < activeProfessionals) {
            int excess = activeProfessionals - target;
            throw new RuntimeException("Você tem " + activeProfessionals + " profissional(is) ativo(s). "
                    + "Desative " + excess + " antes de mudar para esse plano.");
        }

        BigDecimal oldPrice = business.getPlanPrice() != null
                ? business.getPlanPrice()
                : pricingService.monthlyPriceFor(current);
        BigDecimal newPrice = pricingService.monthlyPriceFor(target);
        String preapprovalId = business.getMpPreapprovalId();

        // 1) Mercado Pago primeiro: se recusar, lança e NADA muda localmente.
        boolean updatedRemote = false;
        if (preapprovalId != null && newPrice.compareTo(oldPrice) != 0) {
            subscriptionService.updateAmount(preapprovalId, newPrice);
            updatedRemote = true;
        }

        // 2) Grava local. Se falhar depois do MP ter mudado, tenta desfazer o MP.
        try {
            business.setTeamSize(target);
            business.setPlanPrice(newPrice);
            businessRepository.save(business);
        } catch (RuntimeException e) {
            if (updatedRemote) {
                try {
                    subscriptionService.updateAmount(preapprovalId, oldPrice);
                } catch (RuntimeException revertError) {
                    log.error("FALHA ao reverter valor da assinatura {} para {} após erro ao salvar plano do negócio {}",
                            preapprovalId, oldPrice, businessId, revertError);
                }
            }
            throw e;
        }

        log.info("Plano do negócio {} alterado de {} para {} profissionais (valor {} -> {})",
                businessId, current, target, oldPrice, newPrice);

        return subscriptionStatusService.toMeResponse(business);
    }

    // Um lock por negócio (interned) — suficiente para uma única instância do backend.
    private static Object lockFor(UUID businessId) {
        return ("plan-change-" + businessId).intern();
    }
}
