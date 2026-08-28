package com.marketplace.backend.repository;

import com.marketplace.backend.entity.TimeOff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TimeOffRepository extends JpaRepository<TimeOff, UUID> {

    List<TimeOff> findByBusinessIdOrderByStartsAtAsc(UUID businessId);

    List<TimeOff> findByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID employeeId, OffsetDateTime endsAt, OffsetDateTime startsAt
    );
}
