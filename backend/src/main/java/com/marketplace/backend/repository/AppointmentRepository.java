package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
        SELECT a
        FROM Appointment a
        WHERE a.employee.id = :employeeId
          AND a.startsAt < :endsAt
          AND a.endsAt > :startsAt
          AND a.status <> :cancelledStatus
    """)
    List<Appointment> findConflictingAppointments(
            @Param("employeeId") UUID employeeId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus
    );

    List<Appointment> findByClientId(UUID clientId);

    List<Appointment> findByBusinessId(UUID businessId);

    List<Appointment> findByEmployeeIdAndStartsAtBetween(
            UUID employeeId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}