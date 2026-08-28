package com.marketplace.backend.repository;

import com.marketplace.backend.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TimeBlockRepository extends JpaRepository<TimeBlock, UUID> {

    List<TimeBlock> findByBusinessIdOrderByStartsAtAsc(UUID businessId);

    List<TimeBlock> findByBusinessIdAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID businessId, OffsetDateTime endsAt, OffsetDateTime startsAt
    );
}
