package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Report;
import com.marketplace.backend.enums.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, ReportTargetType targetType, UUID targetId);
}
