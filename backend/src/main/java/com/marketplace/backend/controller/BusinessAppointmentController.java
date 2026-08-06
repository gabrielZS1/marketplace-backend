package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AppointmentResponseDTO;
import com.marketplace.backend.dto.AppointmentStatusUpdateRequestDTO;
import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.BusinessRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/appointments")
public class BusinessAppointmentController {

    // Define quais transições de status são permitidas a partir de cada estado atual
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(AppointmentStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(AppointmentStatus.PENDING, EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.IN_PROGRESS, EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.COMPLETED, EnumSet.noneOf(AppointmentStatus.class));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.NO_SHOW, EnumSet.noneOf(AppointmentStatus.class));
    }

    private final AppointmentRepository appointmentRepository;
    private final BusinessRepository businessRepository;

    public BusinessAppointmentController(AppointmentRepository appointmentRepository, BusinessRepository businessRepository) {
        this.appointmentRepository = appointmentRepository;
        this.businessRepository = businessRepository;
    }

    @GetMapping
    public List<AppointmentResponseDTO> list(
            @PathVariable UUID businessId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) AppointmentStatus status
    ) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        checkIsOwner(business);

        return appointmentRepository.findByBusinessId(businessId).stream()
                .filter(a -> date == null || a.getStartsAt().toLocalDate().equals(date))
                .filter(a -> employeeId == null || a.getEmployee().getId().equals(employeeId))
                .filter(a -> status == null || a.getStatus() == status)
                .sorted((a, b) -> a.getStartsAt().compareTo(b.getStartsAt()))
                .map(this::toResponseDTO)
                .toList();
    }

    @PatchMapping("/{appointmentId}/status")
    public ResponseEntity<AppointmentResponseDTO> updateStatus(
            @PathVariable UUID businessId,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentStatusUpdateRequestDTO request
    ) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        checkIsOwner(business);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (!appointment.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Agendamento não pertence a esta empresa");
        }

        AppointmentStatus current = appointment.getStatus();
        AppointmentStatus next = request.getStatus();

        if (!ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(AppointmentStatus.class)).contains(next)) {
            throw new RuntimeException("Não é possível mudar de " + current + " para " + next);
        }

        appointment.setStatus(next);
        Appointment updated = appointmentRepository.save(appointment);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    private void checkIsOwner(Business business) {
        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }
    }

    private AppointmentResponseDTO toResponseDTO(Appointment a) {
        return new AppointmentResponseDTO(
                a.getId(), a.getBusiness().getId(), a.getBusiness().getName(), a.getClient().getName(),
                a.getEmployee().getUser().getName(), a.getService().getName(), a.getStartsAt(), a.getEndsAt(),
                a.getStatus(), a.getIsHomeService(), a.getNotes()
        );
    }
}