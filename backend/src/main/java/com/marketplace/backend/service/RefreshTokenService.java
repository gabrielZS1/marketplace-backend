package com.marketplace.backend.service;

import com.marketplace.backend.entity.RefreshToken;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.RefreshTokenRepository;
import com.marketplace.backend.security.JwtService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtService.generateRefreshTokenValue());
        refreshToken.setExpiresAt(
                OffsetDateTime.now().plusDays(JwtService.REFRESH_TOKEN_EXPIRATION_DAYS)
        );
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateAndGet(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token inválido"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new RuntimeException("Refresh token revogado");
        }

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Refresh token expirado");
        }

        return refreshToken;
    }

    public void revoke(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}