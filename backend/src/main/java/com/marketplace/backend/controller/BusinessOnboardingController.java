package com.marketplace.backend.controller;

import com.marketplace.backend.dto.onboarding.*;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.PromoCode;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.PromoCodeRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.service.SubscriptionService;
import com.marketplace.backend.service.PricingService;
import com.marketplace.backend.repository.WorkingHourRepository;
import com.marketplace.backend.entity.WorkingHour;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/onboarding")
public class BusinessOnboardingController {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final WorkingHourRepository workingHourRepository;
    private final SubscriptionService subscriptionService;
    private final PromoCodeRepository promoCodeRepository;
    private final PricingService pricingService;

    @Value("${app.subscription.default-trial-days:7}")
    private int defaultTrialDays;

    @Value("${app.auth.require-verified-email:false}")
    private boolean requireVerifiedEmail;

    public BusinessOnboardingController(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            WorkingHourRepository workingHourRepository,
            SubscriptionService subscriptionService,
            PromoCodeRepository promoCodeRepository,
            PricingService pricingService
    ) {

        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.workingHourRepository = workingHourRepository;
        this.subscriptionService = subscriptionService;
        this.promoCodeRepository = promoCodeRepository;
        this.pricingService = pricingService;
    }

    // ============================================================
    // ETAPA 1
    // Escolher categoria
    // ============================================================

    @PostMapping("/start")
    public ResponseEntity<OnboardingStatusResponseDTO> start(
            @Valid @RequestBody StartOnboardingRequestDTO request
    ) {

        User owner = userRepository.findById(getLoggedUserId())
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        /*
         * Primeiro verifica se já existe uma empresa
         * incompleta para esse usuário.
         *
         * Assim evitamos criar várias empresas
         * toda vez que o usuário entrar novamente
         * no cadastro.
         */

        Business existingBusiness =
                businessRepository.findAll().stream()
                        .filter(b ->
                                b.getOwner()
                                        .getId()
                                        .equals(owner.getId())
                        )
                        .filter(b ->
                                !Boolean.TRUE.equals(
                                        b.getOnboardingCompleted()
                                )
                        )
                        .findFirst()
                        .orElse(null);

        if (existingBusiness != null) {

            existingBusiness.setCategory(
                    request.getCategory()
            );

            Business saved =
                    businessRepository.save(
                            existingBusiness
                    );

            return ResponseEntity.ok(
                    new OnboardingStatusResponseDTO(
                            saved.getId(),
                            saved.getOnboardingCompleted()
                    )
            );
        }

        /*
         * Se não existe uma empresa incompleta,
         * cria uma nova.
         */

        Business business = new Business();

        business.setOwner(owner);
        business.setCategory(request.getCategory());
        business.setOnboardingCompleted(false);

        // O período de teste começa quando a conta é criada.
        // Se um código promocional for aplicado no fim do onboarding, o prazo
        // é recalculado a partir desta mesma data (createdAt).
        business.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        business.setTrialEndsAt(OffsetDateTime.now().plusDays(defaultTrialDays));

        Business saved =
                businessRepository.save(business);

        return ResponseEntity.ok(
                new OnboardingStatusResponseDTO(
                        saved.getId(),
                        saved.getOnboardingCompleted()
                )
        );
    }

    // ============================================================
    // ETAPA 2
    // Nome comercial + descrição
    // ============================================================

    @PatchMapping("/{id}/basic-info")
    public ResponseEntity<OnboardingStatusResponseDTO> basicInfo(
            @PathVariable UUID id,
            @Valid @RequestBody BasicInfoRequestDTO request
    ) {

        Business business =
                findOwnedBusiness(id);

        business.setName(request.getName());
        business.setDescription(
                request.getDescription()
        );

        businessRepository.save(business);

        return ResponseEntity.ok(
                new OnboardingStatusResponseDTO(
                        business.getId(),
                        business.getOnboardingCompleted()
                )
        );
    }

    // ============================================================
    // ETAPA 3
    // Onde trabalha
    // ============================================================

    @PatchMapping("/{id}/work-location")
    public ResponseEntity<OnboardingStatusResponseDTO> workLocation(
            @PathVariable UUID id,
            @Valid @RequestBody WorkLocationRequestDTO request
    ) {

        Business business =
                findOwnedBusiness(id);

        business.setWorkLocationType(
                request.getWorkLocationType()
        );

        businessRepository.save(business);

        return ResponseEntity.ok(
                new OnboardingStatusResponseDTO(
                        business.getId(),
                        business.getOnboardingCompleted()
                )
        );
    }

    // ============================================================
    // ETAPA 4
    // Endereço
    // ============================================================

    @PatchMapping("/{id}/address")
    public ResponseEntity<OnboardingStatusResponseDTO> address(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequestDTO request
    ) {

        Business business =
                findOwnedBusiness(id);

        business.setAddress(request.getAddress());
        business.setNumber(request.getNumber());
        business.setCity(request.getCity());
        business.setState(request.getState());
        business.setLatitude(request.getLatitude());
        business.setLongitude(request.getLongitude());

        businessRepository.save(business);

        return ResponseEntity.ok(
                new OnboardingStatusResponseDTO(
                        business.getId(),
                        business.getOnboardingCompleted()
                )
        );
    }

    // ============================================================
    // ETAPA 5
    // Tamanho da equipe
    // ============================================================

    @PatchMapping("/{id}/team-size")
    public ResponseEntity<OnboardingStatusResponseDTO> teamSize(
            @PathVariable UUID id,
            @Valid @RequestBody TeamSizeRequestDTO request
    ) {

        Business business =
                findOwnedBusiness(id);

        business.setTeamSize(
                request.getTeamSize()
        );

        business.setPlanPrice(
                pricingService.monthlyPriceFor(
                        request.getTeamSize()
                )
        );

        businessRepository.save(business);

        return ResponseEntity.ok(
                new OnboardingStatusResponseDTO(
                        business.getId(),
                        business.getOnboardingCompleted()
                )
        );
    }

    // ============================================================
    // ETAPA 6
    // Horários de funcionamento
    // ============================================================

    @PutMapping("/{id}/working-hours")
    @Transactional
    public ResponseEntity<OnboardingStatusResponseDTO> workingHours(
            @PathVariable UUID id,
            @Valid @RequestBody WorkingHoursRequestDTO request
    ) {

        Business business =
                findOwnedBusiness(id);

        workingHourRepository.deleteByBusinessId(
                business.getId()
        );

        List<WorkingHour> entries =
                request.getEntries()
                        .stream()
                        .map(entry -> {

                            WorkingHour wh =
                                    new WorkingHour();

                            wh.setBusiness(business);
                            wh.setDayOfWeek(
                                    entry.getDayOfWeek()
                            );
                            wh.setStartTime(
                                    entry.getStartTime()
                            );
                            wh.setEndTime(
                                    entry.getEndTime()
                            );

                            return wh;
                        })
                        .toList();

        workingHourRepository.saveAll(entries);

        return ResponseEntity.ok(
                new OnboardingStatusResponseDTO(
                        business.getId(),
                        business.getOnboardingCompleted()
                )
        );
    }

    // ============================================================
    // ETAPA FINAL
    // Completar onboarding
    // ============================================================

    @PostMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<CompleteOnboardingResponseDTO> complete(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteOnboardingRequestDTO request
    ) {

        Business business =
                findOwnedBusiness(id);

        // Dono precisa ter e-mail confirmado pra concluir (quando a exigência está ligada).
        if (requireVerifiedEmail && !Boolean.TRUE.equals(business.getOwner().getEmailVerified())) {
            throw new RuntimeException("Confirme seu e-mail antes de concluir o cadastro.");
        }

        /*
         * Verifica se todas as informações necessárias
         * foram preenchidas.
         */

        if (
                business.getName() == null ||
                        business.getAddress() == null ||
                        business.getCity() == null ||
                        business.getState() == null ||
                        business.getLatitude() == null ||
                        business.getLongitude() == null ||
                        business.getTeamSize() == null
        ) {

            throw new RuntimeException(
                    "Onboarding incompleto: preencha todas as etapas antes de finalizar."
            );
        }

        /*
         * Marca o cadastro como concluído.
         */

        business.setOnboardingCompleted(true);
        business.setActive(true);

        // O período de teste conta a partir da criação da conta, não do fim do onboarding.
        OffsetDateTime trialAnchor = business.getCreatedAt() != null
                ? business.getCreatedAt()
                : OffsetDateTime.now();

        String paymentUrl = null;

        // ========================================================
        // COM CÓDIGO PROMOCIONAL
        // ========================================================

        if (
                request.getPromoCode() != null &&
                        !request.getPromoCode().isBlank()
        ) {

            PromoCode promo =
                    promoCodeRepository
                            .findByCode(
                                    request.getPromoCode()
                                            .trim()
                                            .toUpperCase()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Código promocional inválido"
                                    )
                            );

            // Resgate atômico — evita dois onboardings resgatarem o mesmo código.
            int redeemed = promoCodeRepository.redeem(
                    promo.getId(), business.getId(), OffsetDateTime.now()
            );

            boolean alreadyMine = false;
            if (redeemed == 0) {
                if (promoCodeRepository.existsByIdAndRedeemedByBusinessId(
                        promo.getId(), business.getId())) {
                    alreadyMine = true; // retry do mesmo dono (toque duplo) — ok
                } else {
                    throw new RuntimeException("Este código promocional já foi utilizado");
                }
            }

            // Trial é 100% controlado por nós. A assinatura no Mercado Pago só é
            // criada quando o cliente for pagar (tela de renovação / POST /subscribe).
            if (!alreadyMine) {
                business.setTrialEndsAt(
                        trialAnchor.plusDays(promo.getFreeDays())
                );
            }

            business.setSubscriptionStatus(
                    SubscriptionStatus.TRIAL
            );

            business.setMpPreapprovalId(null);

        } else {

            // ====================================================
            // SEM CÓDIGO PROMOCIONAL
            // Trial padrão (fora do lançamento): 7 dias a partir da criação da conta
            // ====================================================

            business.setTrialEndsAt(
                    trialAnchor.plusDays(defaultTrialDays)
            );

            business.setSubscriptionStatus(
                    SubscriptionStatus.TRIAL
            );

            business.setMpPreapprovalId(null);
        }

        /*
         * Salva definitivamente a empresa.
         */

        businessRepository.save(business);

        return ResponseEntity.ok(
                new CompleteOnboardingResponseDTO(
                        business.getId(),
                        business.getOnboardingCompleted(),
                        paymentUrl
                )
        );
    }

    // ============================================================
    // UPGRADE DE PLANO
    // ============================================================

    @PatchMapping("/{id}/upgrade-plan")
    public ResponseEntity<CompleteOnboardingResponseDTO> upgradePlan(
            @PathVariable UUID id,
            @Valid @RequestBody TeamSizeRequestDTO request
    ) {

        Business business =
                findOwnedBusiness(id);

        if (
                request.getTeamSize() <=
                        business.getTeamSize()
        ) {

            throw new RuntimeException(
                    "O novo plano precisa ter mais profissionais que o atual."
            );
        }

        subscriptionService.cancelSubscription(
                business.getMpPreapprovalId()
        );

        business.setTeamSize(
                request.getTeamSize()
        );

        business.setPlanPrice(
                pricingService.monthlyPriceFor(
                        request.getTeamSize()
                )
        );

        SubscriptionService.SubscriptionResult result =
                subscriptionService.createSubscription(
                        business,
                        0
                );

        business.setMpPreapprovalId(
                result.preapprovalId()
        );

        // Mantém o status atual; o webhook do Mercado Pago confirma a nova assinatura.

        businessRepository.save(business);

        return ResponseEntity.ok(
                new CompleteOnboardingResponseDTO(
                        business.getId(),
                        business.getOnboardingCompleted(),
                        result.paymentUrl()
                )
        );
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Business findOwnedBusiness(UUID id) {

        Business business =
                businessRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Empresa não encontrada"
                                )
                        );

        UUID loggedUserId =
                getLoggedUserId();

        if (
                !business.getOwner()
                        .getId()
                        .equals(loggedUserId)
        ) {

            throw new RuntimeException(
                    "Você não tem permissão para editar esta empresa"
            );
        }

        return business;
    }

    private UUID getLoggedUserId() {

        String userId =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return UUID.fromString(userId);
    }
}