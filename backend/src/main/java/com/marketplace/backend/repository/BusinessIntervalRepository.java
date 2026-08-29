package com.marketplace.backend.repository;

import com.marketplace.backend.entity.BusinessInterval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessIntervalRepository extends JpaRepository<BusinessInterval, UUID> {

    List<BusinessInterval> findByBusinessIdOrderByStartTimeAsc(UUID businessId);
}
