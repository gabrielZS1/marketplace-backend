package com.marketplace.backend.controller;

import com.marketplace.backend.dto.WorkingHourRequestDTO;
import com.marketplace.backend.dto.WorkingHourResponseDTO;
import com.marketplace.backend.dto.WorkingHoursUpdateRequestDTO;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.WorkingHour;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.WorkingHourRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
public class WorkingHourController {

    private final WorkingHourRepository workingHourRepository;
    private final EmployeeRepository employeeRepository;

    public WorkingHourController(
            WorkingHourRepository workingHourRepository,
            EmployeeRepository employeeRepository
    ) {
        this.workingHourRepository = workingHourRepository;
        this.employeeRepository = employeeRepository;
    }

    // =========================================================
    // HORÁRIOS DO FUNCIONÁRIO
    // =========================================================

    @GetMapping("/api/employees/{employeeId}/working-hours")
    public List<WorkingHourResponseDTO> listByEmployee(
            @PathVariable UUID employeeId
    ) {

        Employee employee = getEmployeeAndCheckPermission(employeeId);

        return workingHourRepository
                .findByEmployeeId(employee.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // =========================================================
    // SALVAR / SUBSTITUIR HORÁRIOS DO FUNCIONÁRIO
    // =========================================================

    @PutMapping("/api/employees/{employeeId}/working-hours")
    public ResponseEntity<List<WorkingHourResponseDTO>> updateEmployeeHours(
            @PathVariable UUID employeeId,
            @Valid @RequestBody WorkingHoursUpdateRequestDTO request
    ) {

        Employee employee = getEmployeeAndCheckPermission(employeeId);

        List<WorkingHourRequestDTO> requestedHours =
                request.getWorkingHours();

        // Validação
        for (WorkingHourRequestDTO hour : requestedHours) {

            if (hour.getStartTime() == null
                    || hour.getEndTime() == null) {

                throw new RuntimeException(
                        "Horário de início e fim são obrigatórios"
                );
            }

            if (!hour.getEndTime().isAfter(hour.getStartTime())) {

                throw new RuntimeException(
                        "O horário final deve ser maior que o horário inicial"
                );
            }
        }

        // Não permitir dois horários para o mesmo dia
        long distinctDays = requestedHours
                .stream()
                .map(WorkingHourRequestDTO::getDayOfWeek)
                .distinct()
                .count();

        if (distinctDays != requestedHours.size()) {

            throw new RuntimeException(
                    "Não é permitido cadastrar mais de um horário para o mesmo dia"
            );
        }

        // Limpa os horários específicos atuais
        workingHourRepository
                .deleteAll(
                        workingHourRepository
                                .findByEmployeeId(employeeId)
                );

        // Cria os novos
        List<WorkingHour> savedHours = requestedHours
                .stream()
                .map(hour -> {

                    WorkingHour workingHour =
                            new WorkingHour();

                    workingHour.setEmployee(employee);
                    workingHour.setBusiness(
                            employee.getBusiness()
                    );

                    workingHour.setDayOfWeek(
                            hour.getDayOfWeek()
                    );

                    workingHour.setStartTime(
                            hour.getStartTime()
                    );

                    workingHour.setEndTime(
                            hour.getEndTime()
                    );

                    return workingHour;
                })
                .toList();

        List<WorkingHour> saved =
                workingHourRepository.saveAll(savedHours);

        return ResponseEntity.ok(
                saved.stream()
                        .map(this::toResponseDTO)
                        .toList()
        );
    }

    // =========================================================
    // HORÁRIOS DA EMPRESA
    // =========================================================

    @GetMapping("/api/businesses/{businessId}/working-hours")
    public List<WorkingHourResponseDTO> listByBusiness(
            @PathVariable UUID businessId
    ) {

        return workingHourRepository
                .findByBusinessId(businessId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    private Employee getEmployeeAndCheckPermission(
            UUID employeeId
    ) {

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Profissional não encontrado"
                                )
                        );

        UUID loggedUserId =
                UUID.fromString(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName()
                );

        if (!employee.getBusiness()
                .getOwner()
                .getId()
                .equals(loggedUserId)) {

            throw new RuntimeException(
                    "Você não tem permissão para gerenciar este profissional"
            );
        }

        return employee;
    }

    private WorkingHourResponseDTO toResponseDTO(
            WorkingHour wh
    ) {

        UUID employeeId = null;

        if (wh.getEmployee() != null) {
            employeeId = wh.getEmployee().getId();
        }

        return new WorkingHourResponseDTO(
                wh.getId(),
                employeeId,
                wh.getDayOfWeek(),
                wh.getStartTime(),
                wh.getEndTime()
        );
    }
}