package com.marketplace.backend.controller;

import com.marketplace.backend.dto.onboarding.*;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.marketplace.backend.repository.WorkingHourRepository;
import com.marketplace.backend.entity.WorkingHour;
import com.marketplace.backend.dto.onboarding.WorkingHoursRequestDTO;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/onboarding")
public class BusinessOnboardingController {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final WorkingHourRepository workingHourRepository;
    private final SubscriptionService subscriptionService;

    public BusinessOnboardingController(BusinessRepository businessRepository, UserRepository userRepository,
                                        WorkingHourRepository workingHourRepository, SubscriptionService subscriptionService) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.workingHourRepository = workingHourRepository;
        this.subscriptionService = subscriptionService;
    }

    // Etapa 1: escolher categoria → cria o registro
    @PostMapping("/start")
    public ResponseEntity<OnboardingStatusResponseDTO> start(@Valid @RequestBody StartOnboardingRequestDTO request) {
        User owner = userRepository.findById(getLoggedUserId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Business business = new Business();
        business.setOwner(owner);
        business.setCategory(request.getCategory());
        business.setOnboardingCompleted(false);

        Business saved = businessRepository.save(business);
        return ResponseEntity.ok(new OnboardingStatusResponseDTO(saved.getId(), saved.getOnboardingCompleted()));
    }

    // Etapa 2: nome comercial + descrição
    @PatchMapping("/{id}/basic-info")
    public ResponseEntity<OnboardingStatusResponseDTO> basicInfo(@PathVariable UUID id, @Valid @RequestBody BasicInfoRequestDTO request) {
        Business business = findOwnedBusiness(id);
        business.setName(request.getName());
        business.setDescription(request.getDescription());
        businessRepository.save(business);
        return ResponseEntity.ok(new OnboardingStatusResponseDTO(business.getId(), business.getOnboardingCompleted()));
    }

    // Etapa 3: onde trabalha
    @PatchMapping("/{id}/work-location")
    public ResponseEntity<OnboardingStatusResponseDTO> workLocation(@PathVariable UUID id, @Valid @RequestBody WorkLocationRequestDTO request) {
        Business business = findOwnedBusiness(id);
        business.setWorkLocationType(request.getWorkLocationType());
        businessRepository.save(business);
        return ResponseEntity.ok(new OnboardingStatusResponseDTO(business.getId(), business.getOnboardingCompleted()));
    }

    // Etapa 4: endereço
    @PatchMapping("/{id}/address")
    public ResponseEntity<OnboardingStatusResponseDTO> address(@PathVariable UUID id, @Valid @RequestBody AddressRequestDTO request) {
        Business business = findOwnedBusiness(id);
        business.setAddress(request.getAddress());
        business.setCity(request.getCity());
        business.setState(request.getState());
        business.setLatitude(request.getLatitude());
        business.setLongitude(request.getLongitude());
        businessRepository.save(business);
        return ResponseEntity.ok(new OnboardingStatusResponseDTO(business.getId(), business.getOnboardingCompleted()));
    }

    // Etapa 5: tamanho da equipe → já calcula o preço do plano
    @PatchMapping("/{id}/team-size")
    public ResponseEntity<OnboardingStatusResponseDTO> teamSize(@PathVariable UUID id, @Valid @RequestBody TeamSizeRequestDTO request) {
        Business business = findOwnedBusiness(id);
        business.setTeamSize(request.getTeamSize());
        business.setPlanPrice(calculatePlanPrice(request.getTeamSize()));
        businessRepository.save(business);
        return ResponseEntity.ok(new OnboardingStatusResponseDTO(business.getId(), business.getOnboardingCompleted()));
    }

    // Etapa final: valida tudo, publica o negócio e cria a assinatura no Mercado Pago
    @PostMapping("/{id}/complete")
    public ResponseEntity<CompleteOnboardingResponseDTO> complete(@PathVariable UUID id, @Valid @RequestBody CompleteOnboardingRequestDTO request) {
        Business business = findOwnedBusiness(id);

        if (business.getName() == null || business.getAddress() == null
                || business.getCity() == null || business.getState() == null
                || business.getLatitude() == null || business.getLongitude() == null
                || business.getTeamSize() == null) {
            throw new RuntimeException("Onboarding incompleto: preencha todas as etapas antes de finalizar.");
        }

        business.setOnboardingCompleted(true);
        business.setActive(true);
        business.setTrialEndsAt(OffsetDateTime.now().plusMonths(request.getTrialMonths()));

        SubscriptionService.SubscriptionResult result = subscriptionService.createSubscription(business);
        business.setMpPreapprovalId(result.preapprovalId());

        businessRepository.save(business);

        return ResponseEntity.ok(new CompleteOnboardingResponseDTO(
                business.getId(), business.getOnboardingCompleted(), result.paymentUrl()
        ));
    }

    // Upgrade de plano: cancela a assinatura atual no MP e cria uma nova com o valor certo
    @PatchMapping("/{id}/upgrade-plan")
    public ResponseEntity<CompleteOnboardingResponseDTO> upgradePlan(@PathVariable UUID id, @Valid @RequestBody TeamSizeRequestDTO request) {
        Business business = findOwnedBusiness(id);

        if (request.getTeamSize() <= business.getTeamSize()) {
            throw new RuntimeException("O novo plano precisa ter mais profissionais que o atual.");
        }

        subscriptionService.cancelSubscription(business.getMpPreapprovalId());

        business.setTeamSize(request.getTeamSize());
        business.setPlanPrice(calculatePlanPrice(request.getTeamSize()));

        SubscriptionService.SubscriptionResult result = subscriptionService.createSubscription(business);
        business.setMpPreapprovalId(result.preapprovalId());
        business.setSubscriptionStatus(SubscriptionStatus.TRIAL);

        businessRepository.save(business);

        return ResponseEntity.ok(new CompleteOnboardingResponseDTO(
                business.getId(), business.getOnboardingCompleted(), result.paymentUrl()
        ));
    }

    // Etapa 6: horários de funcionamento (com intervalos)
    @PutMapping("/{id}/working-hours")
    @Transactional
    public ResponseEntity<OnboardingStatusResponseDTO> workingHours(@PathVariable UUID id, @Valid @RequestBody WorkingHoursRequestDTO request) {
        Business business = findOwnedBusiness(id);

        workingHourRepository.deleteByBusinessId(business.getId());

        List<WorkingHour> entries = request.getEntries().stream()
                .map(entry -> {
                    WorkingHour wh = new WorkingHour();
                    wh.setBusiness(business);
                    wh.setDayOfWeek(entry.getDayOfWeek());
                    wh.setStartTime(entry.getStartTime());
                    wh.setEndTime(entry.getEndTime());
                    return wh;
                })
                .toList();

        workingHourRepository.saveAll(entries);

        return ResponseEntity.ok(new OnboardingStatusResponseDTO(business.getId(), business.getOnboardingCompleted()));
    }

    // ── Helpers ──

    private Business findOwnedBusiness(UUID id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        if (!business.getOwner().getId().equals(getLoggedUserId())) {
            throw new RuntimeException("Você não tem permissão para editar esta empresa");
        }
        return business;
    }

    private BigDecimal calculatePlanPrice(int teamSize) {
        if (teamSize == 1) return new BigDecimal("10.00");
        if (teamSize == 2) return new BigDecimal("15.00");
        if (teamSize == 3) return new BigDecimal("20.00");
        if (teamSize == 4) return new BigDecimal("30.00");
        return new BigDecimal("40.00");
    }

    private UUID getLoggedUserId() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return UUID.fromString(userId);
    }
}