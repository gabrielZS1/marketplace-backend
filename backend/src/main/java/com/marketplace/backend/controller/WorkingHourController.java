package com.marketplace.backend.controller;

import com.marketplace.backend.dto.WorkingHourRequestDTO;
import com.marketplace.backend.dto.WorkingHourResponseDTO;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.WorkingHour;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.WorkingHourRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class WorkingHourController {

    private final WorkingHourRepository workingHourRepository;
    private final EmployeeRepository employeeRepository;

    public WorkingHourController(WorkingHourRepository workingHourRepository, EmployeeRepository employeeRepository) {
        this.workingHourRepository = workingHourRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/api/employees/{employeeId}/working-hours")
    public List<WorkingHourResponseDTO> listByEmployee(@PathVariable UUID employeeId) {
        return workingHourRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @GetMapping("/api/businesses/{businessId}/working-hours")
    public List<WorkingHourResponseDTO> listByBusiness(@PathVariable UUID businessId) {
        return workingHourRepository.findByEmployeeBusinessId(businessId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @PostMapping("/api/employees/{employeeId}/working-hours")
    public ResponseEntity<WorkingHourResponseDTO> create(@PathVariable UUID employeeId, @Valid @RequestBody WorkingHourRequestDTO request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!employee.getBusiness().getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar este profissional");
        }

        WorkingHour wh = new WorkingHour();
        wh.setEmployee(employee);
        wh.setDayOfWeek(request.getDayOfWeek());
        wh.setStartTime(request.getStartTime());
        wh.setEndTime(request.getEndTime());

        WorkingHour saved = workingHourRepository.save(wh);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    private WorkingHourResponseDTO toResponseDTO(WorkingHour wh) {
        return new WorkingHourResponseDTO(wh.getId(), wh.getEmployee().getId(), wh.getDayOfWeek(), wh.getStartTime(), wh.getEndTime());
    }
}