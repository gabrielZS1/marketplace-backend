package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
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

    /**
     * Fecha de uma vez os agendamentos cujo horário de término já passou:
     * move de {@code from} para {@code to}. Devolve quantos foram alterados.
     */
    @Modifying
    @Query("""
        UPDATE Appointment a
        SET a.status = :to, a.updatedAt = :now
        WHERE a.status IN :from
          AND a.endsAt < :now
    """)
    int closeEnded(
            @Param("from") Collection<AppointmentStatus> from,
            @Param("to") AppointmentStatus to,
            @Param("now") OffsetDateTime now
    );

    List<Appointment> findByClientId(UUID clientId);

    List<Appointment> findByBusinessId(UUID businessId);

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.service
        JOIN FETCH a.employee e
        JOIN FETCH e.user
        JOIN FETCH a.client
        WHERE a.business.id = :businessId
          AND a.startsAt >= :from
          AND a.startsAt < :to
    """)
    List<Appointment> findForStats(
            @Param("businessId") UUID businessId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
        SELECT a.client.id, MIN(a.startsAt)
        FROM Appointment a
        WHERE a.business.id = :businessId
        GROUP BY a.client.id
    """)
    List<Object[]> findFirstAppointmentPerClient(@Param("businessId") UUID businessId);

    List<Appointment> findByEmployeeIdAndStartsAtBetween(
            UUID employeeId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}