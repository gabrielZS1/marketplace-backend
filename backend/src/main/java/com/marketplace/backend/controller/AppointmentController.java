package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AppointmentRequestDTO;
import com.marketplace.backend.dto.AppointmentResponseDTO;
import com.marketplace.backend.entity.*;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;

    public AppointmentController(AppointmentRepository appointmentRepository, UserRepository userRepository,
                                 EmployeeRepository employeeRepository, ServiceRepository serviceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.serviceRepository = serviceRepository;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> create(@Valid @RequestBody AppointmentRequestDTO request) {
        UUID clientId = getLoggedUserId();
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Employee employee;

        if (request.getEmployeeId() != null) {

            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        } else {

            Service service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

            Business business = service.getBusiness();

            List<Employee> employees =
                    employeeRepository.findByBusinessIdAndActiveTrue(
                            business.getId()
                    );

            if (employees.isEmpty()) {
                throw new RuntimeException("Nenhum profissional disponível");
            }

            employee = employees.get(0);
        }

        com.marketplace.backend.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        if (!service.getBusiness().getId().equals(employee.getBusiness().getId())) {
            throw new RuntimeException("Este serviço não está disponível com este profissional");
        }

        if (request.getIsHomeService() && (request.getClientAddress() == null || request.getClientAddress().isBlank())) {
            throw new RuntimeException("Informe o endereço para atendimento a domicílio");
        }

        Appointment appointment = new Appointment();
        appointment.setBusiness(employee.getBusiness());
        appointment.setClient(client);
        appointment.setEmployee(employee);
        appointment.setService(service);
        appointment.setStartsAt(request.getStartsAt());
        appointment.setEndsAt(request.getStartsAt().plusMinutes(service.getDurationMinutes()));
        appointment.setIsHomeService(request.getIsHomeService());
        appointment.setClientAddress(request.getClientAddress());
        appointment.setNotes(request.getNotes());
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved = appointmentRepository.save(appointment);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @GetMapping("/me")
    public List<AppointmentResponseDTO> listMyAppointments() {

        UUID clientId = getLoggedUserId();

        System.out.println("Usuário logado: " + clientId);

        return appointmentRepository.findByClientId(clientId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancel(@PathVariable UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        UUID loggedUserId = getLoggedUserId();
        if (!appointment.getClient().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para cancelar este agendamento");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    private UUID getLoggedUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private AppointmentResponseDTO toResponseDTO(Appointment a) {
        return new AppointmentResponseDTO(
                a.getId(), a.getBusiness().getId(), a.getBusiness().getName(), a.getClient().getName(),
                a.getEmployee().getUser().getName(), a.getService().getName(), a.getStartsAt(), a.getEndsAt(),
                a.getStatus(), a.getIsHomeService(), a.getNotes()
        );
    }
}