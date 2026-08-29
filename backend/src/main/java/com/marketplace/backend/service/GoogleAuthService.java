package com.marketplace.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Valida o ID token do "Entrar com Google" contra as chaves públicas do Google
 * e confere se a audience é um dos client IDs OAuth do app (Web / iOS / Android).
 */
@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final boolean enabled;

    public GoogleAuthService(@Value("${google.oauth.client-ids:}") String clientIds) {
        List<String> audiences = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        this.enabled = !audiences.isEmpty();
        this.verifier = enabled
                ? new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(audiences)
                        .build()
                : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public record GoogleUser(String googleId, String email, String name, String picture, boolean emailVerified) {}

    public GoogleUser verify(String idTokenString) {
        if (!enabled) {
            throw new RuntimeException("Login com Google não está disponível.");
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível validar o login com Google.");
        }

        if (idToken == null) {
            throw new RuntimeException("Token do Google inválido.");
        }

        GoogleIdToken.Payload p = idToken.getPayload();
        String email = p.getEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("A conta do Google não tem e-mail.");
        }

        return new GoogleUser(
                p.getSubject(),
                email.toLowerCase(),
                (String) p.get("name"),
                (String) p.get("picture"),
                Boolean.TRUE.equals(p.getEmailVerified())
        );
    }
}
