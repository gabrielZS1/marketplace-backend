package com.marketplace.backend.service;

import com.marketplace.backend.entity.RefreshToken;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.RefreshTokenRepository;
import com.marketplace.backend.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    /**
     * Cria um refresh token. Guarda só o hash no banco e devolve o valor cru,
     * que é o que vai pro cliente (e nunca mais é armazenado em lugar nenhum).
     */
    public String createRefreshToken(User user) {
        String raw = jwtService.generateRefreshTokenValue();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256(raw));
        refreshToken.setExpiresAt(
                OffsetDateTime.now().plusDays(JwtService.REFRESH_TOKEN_EXPIRATION_DAYS)
        );
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return raw;
    }

    public RefreshToken validateAndGet(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new RuntimeException("Refresh token inválido"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new RuntimeException("Refresh token revogado");
        }

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Refresh token expirado");
        }

        return refreshToken;
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /** Desloga o usuário de todos os dispositivos (usado ao redefinir a senha). */
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
