package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    List<Business> findBySubscriptionStatus(SubscriptionStatus status);

    @Query(value = """
        SELECT b.*, 
               earth_distance(ll_to_earth(:lat, :lng), ll_to_earth(b.latitude, b.longitude)) / 1000 AS distance_km
        FROM businesses b
        WHERE b.active = true
          AND earth_box(ll_to_earth(:lat, :lng), :radiusMeters) @> ll_to_earth(b.latitude, b.longitude)
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<Object[]> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") double radiusMeters);
}