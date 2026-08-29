package com.marketplace.backend.controller;

import com.marketplace.backend.dto.BusinessIntervalRequestDTO;
import com.marketplace.backend.dto.BusinessIntervalResponseDTO;
import com.marketplace.backend.dto.TimeBlockRequestDTO;
import com.marketplace.backend.dto.TimeBlockResponseDTO;
import com.marketplace.backend.dto.TimeOffRequestDTO;
import com.marketplace.backend.dto.TimeOffResponseDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.BusinessInterval;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.TimeBlock;
import com.marketplace.backend.entity.TimeOff;
import com.marketplace.backend.repository.BusinessIntervalRepository;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.TimeBlockRepository;
import com.marketplace.backend.repository.TimeOffRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bloqueios de horário (estabelecimento todo) e folgas (por profissional).
 * Apenas o dono do estabelecimento gerencia.
 */
@RestController
@RequestMapping("/api/businesses/{businessId}")
public class ScheduleController {

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final TimeOffRepository timeOffRepository;
    private final BusinessIntervalRepository businessIntervalRepository;
    private final com.marketplace.backend.service.BusinessAccessService businessAccessService;

    public ScheduleController(
            BusinessRepository businessRepository,
            EmployeeRepository employeeRepository,
            TimeBlockRepository timeBlockRepository,
            TimeOffRepository timeOffRepository,
            BusinessIntervalRepository businessIntervalRepository,
            com.marketplace.backend.service.BusinessAccessService businessAccessService
    ) {
        this.businessRepository = businessRepository;
        this.employeeRepository = employeeRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.timeOffRepository = timeOffRepository;
        this.businessIntervalRepository = businessIntervalRepository;
        this.businessAccessService = businessAccessService;
    }

    // ===================== INTERVALO (recorrente, todo dia) =====================

    @GetMapping("/intervals")
    public List<BusinessIntervalResponseDTO> listIntervals(@PathVariable UUID businessId) {
        businessAccessService.requireMember(businessId, loggedUserId());
        return businessIntervalRepository.findByBusinessIdOrderByStartTimeAsc(businessId)
                .stream().map(BusinessIntervalResponseDTO::new).toList();
    }

    @PostMapping("/intervals")
    public ResponseEntity<BusinessIntervalResponseDTO> createInterval(
            @PathVariable UUID businessId,
            @Valid @RequestBody BusinessIntervalRequestDTO request
    ) {
        Business business = ownedBusiness(businessId);
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("O horário final deve ser depois do inicial");
        }

        BusinessInterval interval = new BusinessInterval();
        interval.setBusiness(business);
        interval.setStartTime(request.getStartTime());
        interval.setEndTime(request.getEndTime());
        interval.setLabel(trimToNull(request.getLabel()));

        return ResponseEntity.ok(
                new BusinessIntervalResponseDTO(businessIntervalRepository.save(interval))
        );
    }

    @DeleteMapping("/intervals/{id}")
    public ResponseEntity<Void> deleteInterval(
            @PathVariable UUID businessId,
            @PathVariable UUID id
    ) {
        ownedBusiness(businessId);
        BusinessInterval interval = businessIntervalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Intervalo não encontrado"));
        if (!interval.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Intervalo não pertence a este estabelecimento");
        }
        businessIntervalRepository.delete(interval);
        return ResponseEntity.noContent().build();
    }

    // ===================== BLOQUEIOS =====================

    @PostMapping("/time-blocks")
    public ResponseEntity<TimeBlockResponseDTO> createTimeBlock(
            @PathVariable UUID businessId,
            @Valid @RequestBody TimeBlockRequestDTO request
    ) {
        Business business = ownedBusiness(businessId);
        validateRange(request.getStartsAt(), request.getEndsAt());

        TimeBlock block = new TimeBlock();
        block.setBusiness(business);
        block.setStartsAt(request.getStartsAt());
        block.setEndsAt(request.getEndsAt());
        block.setReason(trimToNull(request.getReason()));

        return ResponseEntity.ok(new TimeBlockResponseDTO(timeBlockRepository.save(block)));
    }

    @GetMapping("/time-blocks")
    public List<TimeBlockResponseDTO> listTimeBlocks(@PathVariable UUID businessId) {
        businessAccessService.requireMember(businessId, loggedUserId());
        return timeBlockRepository.findByBusinessIdOrderByStartsAtAsc(businessId)
                .stream().map(TimeBlockResponseDTO::new).toList();
    }

    @DeleteMapping("/time-blocks/{id}")
    public ResponseEntity<Void> deleteTimeBlock(
            @PathVariable UUID businessId,
            @PathVariable UUID id
    ) {
        ownedBusiness(businessId);
        TimeBlock block = timeBlockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bloqueio não encontrado"));
        if (!block.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Bloqueio não pertence a este estabelecimento");
        }
        timeBlockRepository.delete(block);
        return ResponseEntity.noContent().build();
    }

    // ===================== FOLGAS =====================

    @PostMapping("/time-offs")
    public ResponseEntity<TimeOffResponseDTO> createTimeOff(
            @PathVariable UUID businessId,
            @Valid @RequestBody TimeOffRequestDTO request
    ) {
        Business business = ownedBusiness(businessId);
        validateRange(request.getStartsAt(), request.getEndsAt());

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        if (!employee.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Profissional não pertence a este estabelecimento");
        }

        TimeOff off = new TimeOff();
        off.setBusiness(business);
        off.setEmployee(employee);
        off.setStartsAt(request.getStartsAt());
        off.setEndsAt(request.getEndsAt());
        off.setAllDay(Boolean.TRUE.equals(request.getAllDay()));
        off.setReason(trimToNull(request.getReason()));

        return ResponseEntity.ok(new TimeOffResponseDTO(timeOffRepository.save(off)));
    }

    @GetMapping("/time-offs")
    public List<TimeOffResponseDTO> listTimeOffs(@PathVariable UUID businessId) {
        businessAccessService.requireMember(businessId, loggedUserId());
        return timeOffRepository.findByBusinessIdOrderByStartsAtAsc(businessId)
                .stream().map(TimeOffResponseDTO::new).toList();
    }

    @DeleteMapping("/time-offs/{id}")
    public ResponseEntity<Void> deleteTimeOff(
            @PathVariable UUID businessId,
            @PathVariable UUID id
    ) {
        ownedBusiness(businessId);
        TimeOff off = timeOffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folga não encontrada"));
        if (!off.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Folga não pertence a este estabelecimento");
        }
        timeOffRepository.delete(off);
        return ResponseEntity.noContent().build();
    }

    // ===================== HELPERS =====================

    private UUID loggedUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Business ownedBusiness(UUID businessId) {
        return businessAccessService.requireOwner(businessId, loggedUserId());
    }

    private void validateRange(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new RuntimeException("O horário final deve ser depois do inicial");
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
