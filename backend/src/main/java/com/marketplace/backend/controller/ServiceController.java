package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ServiceRequestDTO;
import com.marketplace.backend.dto.ServiceResponseDTO;
import com.marketplace.backend.dto.ServiceUpdateRequestDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.ServiceRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/services")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;

    public ServiceController(ServiceRepository serviceRepository, BusinessRepository businessRepository) {
        this.serviceRepository = serviceRepository;
        this.businessRepository = businessRepository;
    }

    @GetMapping
    public List<ServiceResponseDTO> listByBusiness(@PathVariable UUID businessId) {
        return serviceRepository.findAll().stream()
                .filter(s -> s.getBusiness().getId().equals(businessId) && s.getActive())
                .map(this::toResponseDTO)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ServiceResponseDTO> create(@PathVariable UUID businessId, @Valid @RequestBody ServiceRequestDTO request) {

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        checkIsOwner(business);

        com.marketplace.backend.entity.Service service = new com.marketplace.backend.entity.Service();
        service.setBusiness(business);
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setLocationType(request.getLocationType());
        service.setCategory(request.getCategory());

        var saved = serviceRepository.save(service);

        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @PatchMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> update(@PathVariable UUID businessId, @PathVariable UUID serviceId,
                                                     @RequestBody ServiceUpdateRequestDTO request) {
        var service = findOwnedService(businessId, serviceId);

        if (request.getName() != null) service.setName(request.getName());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getPrice() != null) service.setPrice(request.getPrice());
        if (request.getDurationMinutes() != null) service.setDurationMinutes(request.getDurationMinutes());
        if (request.getLocationType() != null) service.setLocationType(request.getLocationType());
        if (request.getCategory() != null) service.setCategory(request.getCategory());
        if (request.getPopular() != null) service.setPopular(request.getPopular());

        var saved = serviceRepository.save(service);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID businessId, @PathVariable UUID serviceId) {
        var service = findOwnedService(businessId, serviceId);
        service.setActive(false);
        serviceRepository.save(service);
        return ResponseEntity.noContent().build();
    }

    private com.marketplace.backend.entity.Service findOwnedService(UUID businessId, UUID serviceId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        checkIsOwner(business);

        var service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        if (!service.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Serviço não pertence a esta empresa");
        }
        return service;
    }

    private void checkIsOwner(Business business) {
        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }
    }

    private ServiceResponseDTO toResponseDTO(com.marketplace.backend.entity.Service s) {
        return new ServiceResponseDTO(s.getId(), s.getName(), s.getDescription(), s.getPrice(), s.getDurationMinutes(), s.getLocationType(), s.getCategory());
    }
}