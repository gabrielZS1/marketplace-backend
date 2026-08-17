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

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        System.out.println("=================================");
        System.out.println("🔥 JWT FILTER");
        System.out.println("REQUEST: "
                + request.getMethod()
                + " "
                + request.getRequestURI());

        System.out.println(
                "AUTH HEADER: "
                        + (authHeader != null ? "EXISTE" : "NULL")
        );

        System.out.println("=================================");

        /*
         * Se não existe Authorization,
         * deixa o Spring Security decidir se a rota é pública ou protegida.
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            System.out.println(
                    "⚠️ Nenhum Bearer Token encontrado."
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        System.out.println(
                "🔥 TOKEN RECEBIDO. TAMANHO: "
                        + token.length()
        );

        try {

            Claims claims = jwtService.parseToken(token);

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            System.out.println("=================================");
            System.out.println("✅ JWT VALIDADO");
            System.out.println("USER ID: " + userId);
            System.out.println("ROLE: " + role);
            System.out.println("=================================");

            if (userId == null || userId.isBlank()) {

                System.out.println(
                        "❌ JWT não possui USER ID."
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(request, response);
                return;
            }

            if (role == null || role.isBlank()) {

                System.out.println(
                        "❌ JWT não possui ROLE."
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(request, response);
                return;
            }

            var authorities = List.of(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role
                    )
            );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "✅ Authentication criada no SecurityContext."
            );

            System.out.println(
                    "Authorities: " + authorities
            );

        } catch (Exception e) {

            System.out.println("=================================");
            System.out.println("❌ ERRO AO VALIDAR JWT");
            System.out.println("MENSAGEM: " + e.getMessage());
            System.out.println("=================================");

            e.printStackTrace();

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}