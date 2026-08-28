package com.marketplace.backend.controller;

import com.marketplace.backend.dto.BusinessDetailResponseDTO;
import com.marketplace.backend.dto.BusinessMeResponseDTO;
import com.marketplace.backend.dto.BusinessRequestDTO;
import com.marketplace.backend.dto.BusinessResponseDTO;
import com.marketplace.backend.dto.BusinessUpdateRequestDTO;
import com.marketplace.backend.dto.ClientResponseDTO;
import com.marketplace.backend.dto.UpdateBusinessStatusDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.BusinessPhoto;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.BusinessCategory;
import com.marketplace.backend.enums.PhotoCategory;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.BusinessPhotoRepository;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.ReviewRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.service.BusinessAccessService;
import com.marketplace.backend.service.FileStorageService;
import com.marketplace.backend.service.SubscriptionStatusService;


import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final BusinessPhotoRepository businessPhotoRepository;
    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final FileStorageService fileStorageService;
    private final SubscriptionStatusService subscriptionStatusService;
    private final EmployeeRepository employeeRepository;
    private final BusinessAccessService businessAccessService;


    public BusinessController(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            BusinessPhotoRepository businessPhotoRepository,
            ReviewRepository reviewRepository,
            AppointmentRepository appointmentRepository,
            FileStorageService fileStorageService,
            SubscriptionStatusService subscriptionStatusService,
            EmployeeRepository employeeRepository,
            BusinessAccessService businessAccessService
    ) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.businessPhotoRepository = businessPhotoRepository;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.fileStorageService = fileStorageService;
        this.subscriptionStatusService = subscriptionStatusService;
        this.employeeRepository = employeeRepository;
        this.businessAccessService = businessAccessService;

    }

    // =========================================================
    // LISTAR TODAS AS EMPRESAS
    // =========================================================

    @GetMapping
    public List<BusinessResponseDTO> listAll(
            @RequestParam(required = false) String category
    ) {

        return businessRepository.findAll().stream()
                .filter(Business::getActive)
                .filter(Business::getOnboardingCompleted)
                .filter(b ->
                        category == null ||
                                b.getCategory().name().equals(category)
                )
                .map(b -> toResponseDTO(b, null))
                .toList();
    }

    // =========================================================
    // EMPRESAS PRÓXIMAS
    // =========================================================

    @GetMapping("/nearby")
    public List<BusinessResponseDTO> findNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10000") double radiusMeters
    ) {

        List<Object[]> results =
                businessRepository.findNearby(
                        lat,
                        lng,
                        radiusMeters
                );

        return results.stream()
                .map(row -> new BusinessResponseDTO(
                        (UUID) row[0],
                        (String) row[2],
                        BusinessCategory.valueOf((String) row[3]),
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        (String) row[7],
                        (String) row[8],
                        ((Number) row[9]).doubleValue(),
                        ((Number) row[10]).doubleValue(),
                        ((Number) row[row.length - 1]).doubleValue()
                ))
                .toList();
    }

    // =========================================================
    // MINHAS EMPRESAS
    // =========================================================

    @GetMapping("/mine")
    public List<BusinessResponseDTO> listMyBusinesses() {

        UUID ownerId = getLoggedUserId();

        return businessRepository.findAll().stream()
                .filter(b ->
                        b.getOwner().getId().equals(ownerId)
                )
                .map(b -> toResponseDTO(b, null))
                .toList();
    }

    // =========================================================
    // CLIENTES DA EMPRESA
    // =========================================================

    @GetMapping("/{id}/clients")
    public List<ClientResponseDTO> listClients(
            @PathVariable UUID id
    ) {

        businessAccessService.requireMember(id, getLoggedUserId());

        Map<UUID, ClientResponseDTO> clientsMap =
                new LinkedHashMap<>();

        appointmentRepository.findAll().stream()
                .filter(a ->
                        a.getBusiness().getId().equals(id)
                )
                .forEach(a -> {

                    User client = a.getClient();

                    clientsMap.putIfAbsent(
                            client.getId(),
                            new ClientResponseDTO(
                                    client.getId(),
                                    client.getName(),
                                    client.getPhone(),
                                    client.getPhotoUrl()
                            )
                    );
                });

        return new ArrayList<>(clientsMap.values());
    }

    // =========================================================
    // CRIAR EMPRESA
    // =========================================================

    @PostMapping
    public ResponseEntity<BusinessResponseDTO> create(
            @Valid @RequestBody BusinessRequestDTO request
    ) {

        UUID ownerId = getLoggedUserId();

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        Business business = new Business();

        business.setOwner(owner);
        business.setName(request.getName());
        business.setCategory(request.getCategory());
        business.setDescription(request.getDescription());
        business.setAddress(request.getAddress());
        business.setCity(request.getCity());
        business.setState(request.getState());
        business.setLatitude(request.getLatitude());
        business.setLongitude(request.getLongitude());

        Business saved =
                businessRepository.save(business);

        return ResponseEntity.ok(
                toResponseDTO(saved, null)
        );
    }

    // =========================================================
    // DETALHES DA EMPRESA
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<BusinessDetailResponseDTO> getById(
            @PathVariable UUID id
    ) {

        Business business = businessRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Empresa não encontrada"
                        )
                );

        return ResponseEntity.ok(toDetailDTO(business));
    }

    // =========================================================
    // ATUALIZAR DADOS DA EMPRESA (dono)
    // =========================================================

    @PatchMapping("/{id}/details")
    public ResponseEntity<BusinessDetailResponseDTO> updateDetails(
            @PathVariable UUID id,
            @RequestBody BusinessUpdateRequestDTO request
    ) {

        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = getLoggedUserId();
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para editar esta empresa");
        }

        if (request.getName() != null) business.setName(request.getName());
        if (request.getDescription() != null) business.setDescription(request.getDescription());
        if (request.getPhone() != null) business.setPhone(request.getPhone());
        if (request.getInstagramUrl() != null) business.setInstagramUrl(request.getInstagramUrl());
        if (request.getTiktokUrl() != null) business.setTiktokUrl(request.getTiktokUrl());
        if (request.getAddress() != null) business.setAddress(request.getAddress());
        if (request.getCity() != null) business.setCity(request.getCity());
        if (request.getState() != null) business.setState(request.getState());
        if (request.getLatitude() != null) business.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) business.setLongitude(request.getLongitude());
        if (request.getWorkLocationType() != null) business.setWorkLocationType(request.getWorkLocationType());
        if (request.getHasParking() != null) business.setHasParking(request.getHasParking());
        if (request.getAllowsPets() != null) business.setAllowsPets(request.getAllowsPets());
        if (request.getHasWifi() != null) business.setHasWifi(request.getHasWifi());

        businessRepository.save(business);

        return ResponseEntity.ok(toDetailDTO(business));
    }

    private BusinessDetailResponseDTO toDetailDTO(Business business) {

        UUID id = business.getId();

        List<BusinessPhoto> allPhotos =
                businessPhotoRepository
                        .findByBusinessIdOrderByPosition(id);

        List<String> photos =
                allPhotos.stream()
                        .map(BusinessPhoto::getUrl)
                        .toList();

        List<String> workspacePhotos =
                allPhotos.stream()
                        .filter(p ->
                                p.getCategory() ==
                                        PhotoCategory.WORKSPACE
                        )
                        .map(BusinessPhoto::getUrl)
                        .toList();

        List<String> portfolioPhotos =
                allPhotos.stream()
                        .filter(p ->
                                p.getCategory() ==
                                        PhotoCategory.PORTFOLIO
                        )
                        .map(BusinessPhoto::getUrl)
                        .toList();

        Double rating =
                reviewRepository
                        .findAverageRatingByBusinessId(id);

        long reviewCount =
                reviewRepository
                        .countByBusinessId(id);

        return new BusinessDetailResponseDTO(
                business.getId(),
                business.getName(),
                business.getCategory(),
                business.getDescription(),
                business.getLogoUrl(),
                business.getAddress(),
                business.getCity(),
                business.getState(),
                business.getLatitude(),
                business.getLongitude(),

                photos,
                workspacePhotos,
                portfolioPhotos,

                rating,
                reviewCount,

                business.getFeatured(),

                business.getWorkLocationType(),

                business.getPhone(),
                business.getInstagramUrl(),
                business.getTiktokUrl(),

                business.getHasParking(),
                business.getAllowsPets(),
                business.getHasWifi()
        );
    }

    // =========================================================
    // ATUALIZAR STATUS DA EMPRESA
    // =========================================================

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessResponseDTO> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateBusinessStatusDTO request
    ) {

        Business business =
                businessRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Empresa não encontrada"
                                )
                        );

        if (request.getActive() != null) {
            business.setActive(
                    request.getActive()
            );
        }

        if (request.getSubscriptionStatus() != null) {
            business.setSubscriptionStatus(
                    request.getSubscriptionStatus()
            );
        }

        if (request.getFeatured() != null) {
            business.setFeatured(
                    request.getFeatured()
            );
        }

        Business updated =
                businessRepository.save(business);

        return ResponseEntity.ok(
                toResponseDTO(updated, null)
        );
    }

    // =========================================================
    // STATUS DA EMPRESA DO USUÁRIO LOGADO
    // =========================================================

    @GetMapping("/mine/status")
    public ResponseEntity<BusinessMeResponseDTO> myStatus() {

        UUID userId = getLoggedUserId();

        Business owned = businessRepository
                .findFirstByOwnerIdOrderByOnboardingCompletedDescCreatedAtAsc(userId)
                .orElse(null);
        if (owned != null) {
            return ResponseEntity.ok(subscriptionStatusService.toMeResponse(owned, "OWNER", null));
        }

        return employeeRepository.findByIdAndActiveTrue(userId)
                .map(e -> ResponseEntity.ok(
                        subscriptionStatusService.toMeResponse(e.getBusiness(), "EMPLOYEE", e.getId())
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // =========================================================
    // ADICIONAR FOTO
    // =========================================================

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    public ResponseEntity<String> addPhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PORTFOLIO") PhotoCategory category
    ) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = getLoggedUserId();
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para editar esta empresa");
        }

        String photoUrl = fileStorageService.store(file, "businesses/" + id);

        BusinessPhoto photo = new BusinessPhoto();
        photo.setBusiness(business);
        photo.setUrl(photoUrl);
        photo.setCategory(category);

        int nextPosition = businessPhotoRepository.findByBusinessIdOrderByPosition(id).size();
        photo.setPosition(nextPosition);

        businessPhotoRepository.save(photo);

        return ResponseEntity.ok(photoUrl);
    }

    // =========================================================
    // LISTAR FOTOS (dono) — com id e categoria pra gerenciar
    // =========================================================

    @GetMapping("/{id}/photos")
    public List<java.util.Map<String, Object>> listPhotos(@PathVariable UUID id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = getLoggedUserId();
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para ver estas fotos");
        }

        return businessPhotoRepository.findByBusinessIdOrderByPosition(id).stream()
                .map(p -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("url", p.getUrl());
                    m.put("category", p.getCategory().name());
                    return m;
                })
                .toList();
    }

    // =========================================================
    // REMOVER FOTO (dono)
    // =========================================================

    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable UUID id,
            @PathVariable UUID photoId
    ) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = getLoggedUserId();
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para editar esta empresa");
        }

        BusinessPhoto photo = businessPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!photo.getBusiness().getId().equals(id)) {
            throw new RuntimeException("Foto não pertence a esta empresa");
        }

        businessPhotoRepository.delete(photo);
        return ResponseEntity.noContent().build();
    }

// =========================================================
// ATUALIZAR LOGO DA EMPRESA
// =========================================================

    @PostMapping(value = "/{id}/logo", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        UUID loggedUserId = getLoggedUserId();
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para editar esta empresa");
        }

        String logoUrl = fileStorageService.store(file, "businesses/" + id + "/logo");

        business.setLogoUrl(logoUrl);
        businessRepository.save(business);

        return ResponseEntity.ok(logoUrl);
    }

    // =========================================================
    // CONVERTER BUSINESS PARA RESPONSE DTO
    // =========================================================

    private BusinessResponseDTO toResponseDTO(
            Business b,
            Double distanceKm
    ) {

        return new BusinessResponseDTO(
                b.getId(),
                b.getName(),
                b.getCategory(),
                b.getDescription(),
                b.getLogoUrl(),
                b.getAddress(),
                b.getCity(),
                b.getState(),
                b.getLatitude(),
                b.getLongitude(),
                distanceKm
        );
    }

    // =========================================================
    // USUÁRIO LOGADO
    // =========================================================

    private UUID getLoggedUserId() {

        String userId =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return UUID.fromString(userId);
    }
}