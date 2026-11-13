// UserRepository.java
package com.marketplace.backend.repository;

import com.marketplace.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);

    /** Conta existe e está ativa (não excluída/desativada). Usado a cada requisição autenticada. */
    boolean existsByIdAndActiveTrue(java.util.UUID id);
}
