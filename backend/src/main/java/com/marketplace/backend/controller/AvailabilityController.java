package com.marketplace.backend.controller;

import com.marketplace.backend.dto.TimeSlotDTO;
import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.WorkingHour;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.ServiceRepository;
import com.marketplace.backend.repository.WorkingHourRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@RestController
public class AvailabilityController {

    private static final int SLOT_STEP_MINUTES = 30;
    private static final ZoneOffset OFFSET = ZoneOffset.of("-03:00");

    private final EmployeeRepository employeeRepository;
    private final WorkingHourRepository workingHourRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;

    public AvailabilityController(
            EmployeeRepository employeeRepository,
            WorkingHourRepository workingHourRepository,
            AppointmentRepository appointmentRepository,
            ServiceRepository serviceRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.workingHourRepository = workingHourRepository;
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("/api/businesses/{businessId}/availability")
    public List<TimeSlotDTO> getAvailability(
            @PathVariable UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId,
            @RequestParam(required = false) UUID employeeId
    ) {

        // =========================================================
        // SERVIÇO
        // =========================================================

        com.marketplace.backend.entity.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() ->
                                new RuntimeException("Serviço não encontrado"));

        int duration = service.getDurationMinutes();

        // =========================================================
        // FUNCIONÁRIOS ATIVOS DO ESTABELECIMENTO
        // =========================================================

        List<Employee> employees = employeeRepository.findAll()
                .stream()
                .filter(e ->
                        e.getBusiness() != null
                                && e.getBusiness().getId().equals(businessId)
                )
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .filter(e ->
                        employeeId == null
                                || e.getId().equals(employeeId)
                )
                .toList();

        if (employees.isEmpty()) {
            return List.of();
        }

        System.out.println("### employeeId recebido: " + employeeId);
        System.out.println("### employees após filtro: " + employees.stream().map(Employee::getId).toList());

        // =========================================================
        // DIA DA SEMANA
        //
        // Sistema:
        // Domingo = 0
        // Segunda = 1
        // ...
        // Sábado = 6
        // =========================================================

        int dayOfWeek = date.getDayOfWeek().getValue() % 7;

        // =========================================================
        // HORÁRIOS DO ESTABELECIMENTO
        //
        // Aqui entram os horários que possuem:
        //
        // employeeId = null
        //
        // Exemplo:
        // 09:00 - 18:00
        // 10:00 - 19:00
        // =========================================================

        List<WorkingHour> businessHours =
                workingHourRepository.findByBusinessId(businessId)
                        .stream()
                        .filter(wh ->
                                wh.getDayOfWeek() != null
                                        && wh.getDayOfWeek() == dayOfWeek
                        )
                        .toList();

        // =========================================================
        // MAPA DE HORÁRIOS POR FUNCIONÁRIO
        // =========================================================

        Map<Employee, List<WorkingHour>> employeeWorkingHours =
                new LinkedHashMap<>();

        for (Employee employee : employees) {

            // ---------------------------------------------
            // Horários específicos do funcionário
            // ---------------------------------------------

            List<WorkingHour> specificHours =
                    workingHourRepository.findByEmployeeId(employee.getId())
                            .stream()
                            .filter(wh ->
                                    wh.getDayOfWeek() != null
                                            && wh.getDayOfWeek() == dayOfWeek
                            )
                            .toList();

            // ---------------------------------------------
            // Se possuir horários próprios, usa eles.
            // ---------------------------------------------

            if (!specificHours.isEmpty()) {

                employeeWorkingHours.put(
                        employee,
                        specificHours
                );

            } else {

                // ---------------------------------------------
                // Caso contrário usa os horários gerais
                // do estabelecimento.
                // ---------------------------------------------

                employeeWorkingHours.put(
                        employee,
                        businessHours
                );
            }
        }

        // =========================================================
        // SE NÃO EXISTE HORÁRIO
        // =========================================================

        boolean hasAnyWorkingHour =
                employeeWorkingHours.values()
                        .stream()
                        .anyMatch(list -> !list.isEmpty());

        if (!hasAnyWorkingHour) {
            return List.of();
        }

        // =========================================================
        // GERAR TODOS OS HORÁRIOS POSSÍVEIS
        // =========================================================

        SortedSet<LocalTime> candidateTimes =
                new TreeSet<>();

        for (List<WorkingHour> hours :
                employeeWorkingHours.values()) {

            for (WorkingHour wh : hours) {

                LocalTime time = wh.getStartTime();

                while (!time.plusMinutes(duration)
                        .isAfter(wh.getEndTime())) {

                    candidateTimes.add(time);

                    time = time.plusMinutes(
                            SLOT_STEP_MINUTES
                    );
                }
            }
        }

        // =========================================================
        // HORÁRIO ATUAL
        // =========================================================

        OffsetDateTime now =
                OffsetDateTime.now(OFFSET);

        // =========================================================
        // RESULTADO
        // =========================================================

        List<TimeSlotDTO> result =
                new ArrayList<>();

        for (LocalTime time : candidateTimes) {

            OffsetDateTime slotStart =
                    OffsetDateTime.of(
                            date,
                            time,
                            OFFSET
                    );

            OffsetDateTime slotEnd =
                    slotStart.plusMinutes(duration);

            // -----------------------------------------------------
            // Não mostrar horário que já passou
            // -----------------------------------------------------

            if (slotStart.isBefore(now)) {
                continue;
            }

            Employee freeEmployee = null;

            // =====================================================
            // PROCURAR UM FUNCIONÁRIO LIVRE
            // =====================================================

            for (Map.Entry<Employee, List<WorkingHour>> entry :
                    employeeWorkingHours.entrySet()) {

                Employee employee = entry.getKey();
                List<WorkingHour> hours = entry.getValue();

                // -------------------------------------------------
                // Verificar se o horário cabe em algum expediente
                // -------------------------------------------------

                boolean withinWorkingHours =
                        hours.stream()
                                .anyMatch(wh ->
                                        !time.isBefore(
                                                wh.getStartTime()
                                        )
                                                &&
                                                !time.plusMinutes(duration)
                                                        .isAfter(
                                                                wh.getEndTime()
                                                        )
                                );

                if (!withinWorkingHours) {
                    continue;
                }

                // =================================================
                // BUSCAR AGENDAMENTOS DO FUNCIONÁRIO
                // =================================================

                List<Appointment> appointments =
                        appointmentRepository
                                .findByEmployeeIdAndStartsAtBetween(
                                        employee.getId(),

                                        OffsetDateTime.of(
                                                date,
                                                LocalTime.MIN,
                                                OFFSET
                                        ),

                                        OffsetDateTime.of(
                                                date,
                                                LocalTime.MAX,
                                                OFFSET
                                        )
                                );

                // =================================================
                // VERIFICAR CONFLITO
                // =================================================

                boolean isFree =
                        appointments.stream()
                                .filter(a ->
                                        a.getStatus()
                                                == AppointmentStatus.PENDING
                                                ||
                                                a.getStatus()
                                                        == AppointmentStatus.CONFIRMED
                                                ||
                                                a.getStatus()
                                                        == AppointmentStatus.IN_PROGRESS
                                )
                                .noneMatch(a ->
                                        a.getStartsAt()
                                                .isBefore(slotEnd)
                                                &&
                                                a.getEndsAt()
                                                        .isAfter(slotStart)
                                );

                // =================================================
                // ENCONTROU FUNCIONÁRIO LIVRE
                // =================================================

                if (isFree) {

                    freeEmployee = employee;

                    break;
                }
            }

            // =====================================================
            // CRIAR SLOT
            // =====================================================

            if (freeEmployee != null) {

                result.add(
                        new TimeSlotDTO(
                                time.toString(),
                                true,
                                freeEmployee.getId(),
                                freeEmployee.getUser().getName()
                        )
                );

            } else {

                result.add(
                        new TimeSlotDTO(
                                time.toString(),
                                false,
                                null,
                                null
                        )
                );
            }
        }

        return result;
    }
}