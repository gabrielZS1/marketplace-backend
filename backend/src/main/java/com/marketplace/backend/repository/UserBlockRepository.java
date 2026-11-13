package com.marketplace.backend.repository;

import com.marketplace.backend.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdOrBlockedId(UUID blockerId, UUID blockedId);

    @Query("SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :blockerId")
    List<UUID> findBlockedIdsByBlockerId(@Param("blockerId") UUID blockerId);

    @Query("""
        SELECT b FROM UserBlock b
        JOIN FETCH b.blocked
        WHERE b.blocker.id = :blockerId
        ORDER BY b.createdAt DESC
    """)
    List<UserBlock> findWithBlockedByBlockerId(@Param("blockerId") UUID blockerId);
}
