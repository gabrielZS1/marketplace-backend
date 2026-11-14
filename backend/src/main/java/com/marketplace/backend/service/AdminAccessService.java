package com.marketplace.backend.service;

import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.UserRole;
import com.marketplace.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Quem pode usar as ferramentas internas (moderação, códigos promocionais).
 *
 * Vale para: conta com papel ADMIN, ou a conta cujo e-mail é o ADMIN_EMAIL configurado no
 * servidor. Nesse segundo caso o e-mail precisa estar verificado (senão alguém poderia criar
 * uma conta com o e-mail do administrador antes dele) e o ADMIN_EMAIL não pode ser o padrão.
 */
@Service
public class AdminAccessService {

    /** Valor padrão do application.properties quando ADMIN_EMAIL não está definido. */
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";

    private final UserRepository userRepository;
    private final String adminEmail;

    public AdminAccessService(UserRepository userRepository,
                              @Value("${app.admin-email:}") String adminEmail) {
        this.userRepository = userRepository;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim();
    }

    /** Lança "Sem permissão" (403) se quem chamou não é administrador. Devolve o admin. */
    public User requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Sem permissão");
        }

        UUID userId;
        try {
            userId = UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Sem permissão");
        }

        User user = userRepository.findById(userId)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(() -> new RuntimeException("Sem permissão"));

        if (user.getRole() == UserRole.ADMIN) {
            return user;
        }

        boolean adminEmailConfigured = !adminEmail.isBlank()
                && !DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(adminEmail);
        if (adminEmailConfigured
                && adminEmail.equalsIgnoreCase(user.getEmail())
                && Boolean.TRUE.equals(user.getEmailVerified())) {
            return user;
        }

        throw new RuntimeException("Sem permissão");
    }
}
