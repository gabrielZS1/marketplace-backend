package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AppointmentRequestDTO;
import com.marketplace.backend.dto.AppointmentResponseDTO;
import com.marketplace.backend.dto.AppointmentStatusUpdateRequestDTO;
import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.ServiceRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.WorkingHourRepository;
import com.marketplace.backend.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private static final ZoneOffset OFFSET = ZoneOffset.of("-03:00");

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final WorkingHourRepository workingHourRepository;
    private final PushNotificationService pushNotificationService;

    public AppointmentController(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            ServiceRepository serviceRepository,
            BusinessRepository businessRepository,
            WorkingHourRepository workingHourRepository,
            PushNotificationService pushNotificationService, PushNotificationService pushNotificationService1 // <- adicionar aqui
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.serviceRepository = serviceRepository;
        this.businessRepository = businessRepository;
        this.workingHourRepository = workingHourRepository;
        // <- campo novo
        this.pushNotificationService = pushNotificationService1;
    }


    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> create(
            @Valid @RequestBody AppointmentRequestDTO request
    ) {
        UUID clientId = getLoggedUserId();

        User client = userRepository.findById(clientId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado"));

        com.marketplace.backend.entity.Service service =
                serviceRepository.findById(request.getServiceId())
                        .orElseThrow(() ->
                                new RuntimeException("Serviço não encontrado"));

        Business business = service.getBusiness();

        OffsetDateTime startsAt = request.getStartsAt()
                .withOffsetSameInstant(OFFSET);

        OffsetDateTime endsAt =
                startsAt.plusMinutes(service.getDurationMinutes());

        if (!startsAt.isAfter(OffsetDateTime.now(OFFSET))) {
            throw new RuntimeException(
                    "O horário do agendamento deve estar no futuro"
            );
        }

        Employee employee;

        if (request.getEmployeeId() != null) {

            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Profissional não encontrado"
                            ));

            if (!Boolean.TRUE.equals(employee.getActive())) {
                throw new RuntimeException(
                        "Este profissional não está disponível"
                );
            }

            if (!employee.getBusiness().getId().equals(business.getId())) {
                throw new RuntimeException(
                        "Este profissional não pertence a este estabelecimento"
                );
            }

            if (!isWithinWorkingHours(
                    employee,
                    business,
                    startsAt,
                    endsAt
            )) {
                throw new RuntimeException(
                        "O horário escolhido está fora do expediente"
                );
            }

            if (hasConflict(
                    employee.getId(),
                    startsAt,
                    endsAt
            )) {
                throw new RuntimeException(
                        "Este profissional já possui um agendamento neste horário"
                );
            }

        } else {

            List<Employee> employees =
                    employeeRepository.findByBusinessIdAndActiveTrue(
                            business.getId()
                    );

            if (employees.isEmpty()) {
                throw new RuntimeException(
                        "Nenhum profissional disponível"
                );
            }

            employee = employees.stream()
                    .filter(e ->
                            isWithinWorkingHours(
                                    e,
                                    business,
                                    startsAt,
                                    endsAt
                            )
                    )
                    .filter(e ->
                            !hasConflict(
                                    e.getId(),
                                    startsAt,
                                    endsAt
                            )
                    )
                    .findFirst()
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Nenhum profissional disponível neste horário"
                            )
                    );
        }

        if (Boolean.TRUE.equals(request.getIsHomeService())
                && (request.getClientAddress() == null
                || request.getClientAddress().isBlank())) {

            throw new RuntimeException(
                    "Informe o endereço para atendimento a domicílio"
            );
        }

        Appointment appointment = new Appointment();

        appointment.setBusiness(business);
        appointment.setClient(client);
        appointment.setEmployee(employee);
        appointment.setService(service);
        appointment.setStartsAt(startsAt);
        appointment.setEndsAt(endsAt);
        appointment.setIsHomeService(
                Boolean.TRUE.equals(request.getIsHomeService())
        );
        appointment.setClientAddress(request.getClientAddress());
        appointment.setNotes(request.getNotes());
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved =
                appointmentRepository.save(appointment);

        // dispara o push pro dono do estabelecimento; se falhar, não quebra o agendamento
        pushNotificationService.sendAppointmentCreated(
                business.getExpoPushToken(),
                client.getName(),
                service.getName()
        );

        return ResponseEntity.ok(
                toResponseDTO(saved)
        );
    }
    @GetMapping("/me")
    public List<AppointmentResponseDTO> listMyAppointments() {

        UUID clientId = getLoggedUserId();

        return appointmentRepository
                .findByClientId(clientId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @GetMapping("/business/{businessId}")
    public List<AppointmentResponseDTO> listByBusiness(
            @PathVariable UUID businessId
    ) {

        Business business =
                businessRepository.findById(businessId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Empresa não encontrada"
                                ));

        UUID loggedUserId = getLoggedUserId();

        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException(
                    "Você não tem permissão para ver os agendamentos desta empresa"
            );
        }

        return appointmentRepository
                .findByBusinessId(businessId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentStatusUpdateRequestDTO request
    ) {

        Appointment appointment =
                appointmentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Agendamento não encontrado"
                                ));

        UUID loggedUserId = getLoggedUserId();

        if (!appointment.getBusiness()
                .getOwner()
                .getId()
                .equals(loggedUserId)) {

            throw new RuntimeException(
                    "Você não tem permissão para alterar este agendamento"
            );
        }

        AppointmentStatus currentStatus =
                appointment.getStatus();

        AppointmentStatus newStatus =
                request.getStatus();

        if (currentStatus == AppointmentStatus.CANCELLED) {
            throw new RuntimeException(
                    "Não é possível alterar um agendamento cancelado"
            );
        }

        if (currentStatus == AppointmentStatus.COMPLETED) {
            throw new RuntimeException(
                    "Não é possível alterar um agendamento concluído"
            );
        }

        if (currentStatus == newStatus) {
            throw new RuntimeException(
                    "O agendamento já possui este status"
            );
        }

        if (newStatus != AppointmentStatus.CONFIRMED
                && newStatus != AppointmentStatus.CANCELLED) {

            throw new RuntimeException(
                    "Status inválido para esta operação"
            );
        }

        appointment.setStatus(newStatus);

        Appointment updated =
                appointmentRepository.save(appointment);

        return ResponseEntity.ok(
                toResponseDTO(updated)
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancel(
            @PathVariable UUID id
    ) {

        Appointment appointment =
                appointmentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Agendamento não encontrado"
                                ));

        UUID loggedUserId = getLoggedUserId();

        if (!appointment.getClient()
                .getId()
                .equals(loggedUserId)) {

            throw new RuntimeException(
                    "Você não tem permissão para cancelar este agendamento"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.CANCELLED) {

            throw new RuntimeException(
                    "Este agendamento já está cancelado"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.COMPLETED) {

            throw new RuntimeException(
                    "Não é possível cancelar um agendamento concluído"
            );
        }

        appointment.setStatus(
                AppointmentStatus.CANCELLED
        );

        Appointment updated =
                appointmentRepository.save(appointment);

        return ResponseEntity.ok(
                toResponseDTO(updated)
        );
    }

    private boolean hasConflict(
            UUID employeeId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {

        return !appointmentRepository
                .findConflictingAppointments(
                        employeeId,
                        startsAt,
                        endsAt,
                        AppointmentStatus.CANCELLED
                )
                .isEmpty();
    }

    private boolean isWithinWorkingHours(
            Employee employee,
            Business business,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {

        int dayOfWeek =
                startsAt.getDayOfWeek().getValue() % 7;

        var specificHours =
                workingHourRepository
                        .findByEmployeeId(employee.getId())
                        .stream()
                        .filter(wh ->
                                wh.getDayOfWeek() != null
                                        && wh.getDayOfWeek() == dayOfWeek
                        )
                        .toList();

        var hours = specificHours.isEmpty()
                ? workingHourRepository
                .findByBusinessId(business.getId())
                .stream()
                .filter(wh ->
                        wh.getDayOfWeek() != null
                                && wh.getDayOfWeek() == dayOfWeek
                )
                .toList()
                : specificHours;

        if (hours.isEmpty()) {
            return false;
        }

        var startTime = startsAt.toLocalTime();
        var endTime = endsAt.toLocalTime();

        return hours.stream().anyMatch(wh ->
                !startTime.isBefore(
                        wh.getStartTime()
                )
                        && !endTime.isAfter(
                        wh.getEndTime()
                )
        );
    }

    private UUID getLoggedUserId() {

        return UUID.fromString(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName()
        );
    }

    private AppointmentResponseDTO toResponseDTO(
            Appointment a
    ) {

        return new AppointmentResponseDTO(
                a.getId(),
                a.getBusiness().getId(),
                a.getBusiness().getName(),
                a.getClient().getName(),
                a.getEmployee().getUser().getName(),
                a.getService().getName(),
                a.getStartsAt(),
                a.getEndsAt(),
                a.getStatus(),
                a.getIsHomeService(),
                a.getNotes()
        );
    }
}