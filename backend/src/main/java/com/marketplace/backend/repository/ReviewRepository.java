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

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.business.id = :businessId")
    Double findAverageRatingByBusinessId(@Param("businessId") UUID businessId);

    long countByBusinessId(UUID id);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.employee.id = :employeeId")
    Double findAverageRatingByEmployeeId(@Param("employeeId") UUID employeeId);
}