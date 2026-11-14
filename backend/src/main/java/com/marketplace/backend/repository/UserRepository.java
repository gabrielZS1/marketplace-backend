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

    /**
     * Para conta ativa, devolve se ela precisa trocar a senha temporária. Vazio = conta
     * inexistente ou desativada. Uma consulta só por requisição autenticada.
     */
    @org.springframework.data.jpa.repository.Query(
            "select u.mustChangePassword from User u where u.id = :id and u.active = true")
    Optional<Boolean> findMustChangePasswordIfActive(
            @org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
