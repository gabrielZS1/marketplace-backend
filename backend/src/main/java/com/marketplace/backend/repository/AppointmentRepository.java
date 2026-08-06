package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByEmployeeIdAndStartsAtBetween(
            UUID employeeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<Appointment> findByClientId(UUID clientId);

    List<Appointment> findByBusinessId(UUID businessId);

}