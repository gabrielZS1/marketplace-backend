package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByBusinessIdAndActiveTrue(UUID businessId);

    // Employee.id == User.id (@MapsId), então isto busca pelo id do usuário logado.
    Optional<Employee> findByIdAndActiveTrue(UUID id);

}