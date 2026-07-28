package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    Optional<Review> findByAppointmentId(UUID appointmentId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.business.id = :businessId")
    Double findAverageRatingByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.employee.id = :employeeId")
    Double findAverageRatingByEmployeeId(@Param("employeeId") UUID employeeId);

    long countByBusinessId(UUID businessId);
}