package com.marketplace.backend.repository;

import com.marketplace.backend.entity.OneTimeCode;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.OneTimeCodePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, UUID> {

    Optional<OneTimeCode> findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            User user, OneTimeCodePurpose purpose);

    @Modifying
    @Query("update OneTimeCode c set c.usedAt = :now "
            + "where c.user = :user and c.purpose = :purpose and c.usedAt is null")
    void invalidateAll(@Param("user") User user,
                       @Param("purpose") OneTimeCodePurpose purpose,
                       @Param("now") OffsetDateTime now);
}
