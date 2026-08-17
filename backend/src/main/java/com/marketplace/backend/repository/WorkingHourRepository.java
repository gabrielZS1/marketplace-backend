package com.marketplace.backend.repository;

import com.marketplace.backend.entity.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, UUID> {

    List<WorkingHour> findByEmployeeId(UUID employeeId);

    List<WorkingHour> findByEmployeeBusinessId(UUID businessId);

    List<WorkingHour> findByBusinessId(UUID businessId);

    void deleteByBusinessId(UUID businessId);
}