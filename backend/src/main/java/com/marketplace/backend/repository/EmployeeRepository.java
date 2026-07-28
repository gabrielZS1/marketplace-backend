// EmployeeRepository.java
package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
}