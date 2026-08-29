package com.marketplace.backend.repository;

import com.marketplace.backend.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {
    Optional<PromoCode> findByCode(String code);

    List<PromoCode> findByRedeemedFalseOrderByCreatedAtAsc();

    List<PromoCode> findByRedeemedFalseAndLabelOrderByCreatedAtAsc(String label);

    /**
     * Resgate atômico: só marca se ainda não estiver resgatado.
     * Retorna 1 quando ganhou a corrida, 0 quando o código já foi usado.
     */
    @Modifying(flushAutomatically = true)
    @Query("update PromoCode p set p.redeemed = true, "
            + "p.redeemedByBusinessId = :businessId, p.redeemedAt = :now "
            + "where p.id = :id and p.redeemed = false")
    int redeem(@Param("id") UUID id,
               @Param("businessId") UUID businessId,
               @Param("now") OffsetDateTime now);

    boolean existsByIdAndRedeemedByBusinessId(UUID id, UUID redeemedByBusinessId);
}
