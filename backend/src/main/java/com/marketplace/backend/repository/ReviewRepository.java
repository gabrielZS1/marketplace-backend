package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByAppointmentId(UUID appointmentId);

    // ← JOIN FETCH garante que appointment e service são carregados junto
    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.appointment a
        JOIN FETCH a.service
        JOIN FETCH r.employee
        JOIN FETCH r.client
        WHERE r.business.id = :businessId
        ORDER BY r.createdAt DESC
    """)
    List<Review> findByBusinessIdOrderByCreatedAtDesc(@Param("businessId") UUID businessId);

    Double findAverageRatingByBusinessId(UUID id);

    long countByBusinessId(UUID id);

    Double findAverageRatingByEmployeeId(UUID id);
}