package com.marketplace.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Verifica o token do Cloudflare Turnstile (CAPTCHA invisível) no cadastro.
 *
 * Só age quando `turnstile.enabled=true` E há uma secret key configurada —
 * assim dev e testes automatizados seguem sem CAPTCHA.
 */
@Service
public class TurnstileService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileService.class);

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://challenges.cloudflare.com")
            .build();

    private final String secret;
    private final boolean enabled;

    public TurnstileService(
            @Value("${turnstile.secret-key:}") String secret,
            @Value("${turnstile.enabled:false}") boolean enabled
    ) {
        this.secret = secret == null ? "" : secret.trim();
        this.enabled = enabled && !this.secret.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Lança RuntimeException (pt-BR) se o CAPTCHA for inválido. No-op quando desabilitado. */
    public void verify(String token, String remoteIp) {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Confirmação de segurança ausente. Tente de novo.");
        }

        boolean success;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secret);
            form.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                form.add("remoteip", remoteIp);
            }

            Map<?, ?> body = webClient.post()
                    .uri("/turnstile/v0/siteverify")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            success = body != null && Boolean.TRUE.equals(body.get("success"));
            if (!success) {
                log.warn("[Turnstile] verificação negada: {}", body);
            }
        } catch (Exception e) {
            log.error("[Turnstile] erro ao verificar token", e);
            throw new RuntimeException("Não foi possível validar a confirmação de segurança. Tente de novo.");
        }

        if (!success) {
            throw new RuntimeException("Falha na confirmação de segurança. Tente de novo.");
        }
    }
}
