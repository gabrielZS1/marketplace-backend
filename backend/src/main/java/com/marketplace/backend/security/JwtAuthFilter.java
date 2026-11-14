package com.marketplace.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.marketplace.backend.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Sem token: deixa o Spring Security decidir se a rota é pública.
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        boolean mustChangePassword = false;

        try {
            Claims claims = jwtService.parseToken(token);

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // O token é assinado e ainda não expirou, mas a conta pode ter sido excluída ou
            // desativada depois (o token vale por ~30 min). Sem esta checagem, quem excluiu a
            // conta continuava usando a API até o token vencer.
            if (userId != null && !userId.isBlank()) {
                Optional<Boolean> flag = userRepository.findMustChangePasswordIfActive(UUID.fromString(userId));
                if (flag.isEmpty()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                mustChangePassword = Boolean.TRUE.equals(flag.get());
            }

            if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }

        } catch (Exception e) {
            // Token inválido/expirado: segue sem autenticação; a rota decide.
            SecurityContextHolder.clearContext();
        }

        // Quem ainda está com a senha temporária (criada pelo dono) só pode trocar a senha,
        // ver a própria conta e sair. Todo o resto espera a troca.
        if (mustChangePassword && !allowedWithTemporaryPassword(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"error\":\"PASSWORD_CHANGE_REQUIRED\",\"status\":403,"
                            + "\"message\":\"Troque sua senha temporária para continuar.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean allowedWithTemporaryPassword(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!path.startsWith("/api/")) return true;                 // páginas estáticas, uploads
        if (path.startsWith("/api/auth/")) return true;             // me, trocar senha, sair, renovar
        if (path.equals("/api/businesses/me") && "GET".equals(method)) return true;
        return path.equals("/api/businesses/me/push-token") && "PATCH".equals(method);
    }
}
