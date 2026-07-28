package com.marketplace.backend.repository;

import com.marketplace.backend.entity.BusinessPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BusinessPhotoRepository extends JpaRepository<BusinessPhoto, UUID> {
    List<BusinessPhoto> findByBusinessIdOrderByPosition(UUID businessId);
}