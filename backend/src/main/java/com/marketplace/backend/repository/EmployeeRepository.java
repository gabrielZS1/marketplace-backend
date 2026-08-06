package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByBusinessIdAndActiveTrue(UUID businessId);

}