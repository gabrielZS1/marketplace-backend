package com.marketplace.backend.repository;

import com.marketplace.backend.entity.GiftCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GiftCardRepository extends JpaRepository<GiftCard, UUID> {
    List<GiftCard> findByBusinessIdAndActiveTrue(UUID businessId);
}