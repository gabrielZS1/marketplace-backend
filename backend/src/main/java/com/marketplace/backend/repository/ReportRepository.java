package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Report;
import com.marketplace.backend.enums.ReportTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, ReportTargetType targetType, UUID targetId);

    @Query("select r from Report r join fetch r.reporter where r.status = :status order by r.createdAt desc")
    List<Report> findByStatusWithReporter(@Param("status") String status, Pageable pageable);

    long countByStatus(String status);

    /** Fecha todas as denúncias abertas sobre o mesmo conteúdo (uma decisão vale para todas). */
    @Modifying
    @Query("update Report r set r.status = :newStatus "
            + "where r.targetType = :type and r.targetId = :targetId and r.status = 'OPEN'")
    int resolveOpenForTarget(@Param("type") ReportTargetType type,
                             @Param("targetId") UUID targetId,
                             @Param("newStatus") String newStatus);
}
