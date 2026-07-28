// ServiceRepository.java
package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {
}