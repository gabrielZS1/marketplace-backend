package com.marketplace.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Envio de e-mail transacional via API do Resend (https://resend.com/docs).
 *
 * Sem RESEND_API_KEY configurada, os e-mails são apenas logados — o backend
 * continua funcionando em dev e o código enviado aparece no console.
 *
 * O envio é "fire-and-forget" (mesmo padrão do PushNotificationService): não
 * bloqueia a resposta HTTP. Se falhar, fica no log e o usuário pede reenvio.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.resend.com")
            .build();

    private final String apiKey;
    private final String from;

    public EmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:Glowly <onboarding@resend.dev>}") String from
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public void sendPasswordResetCode(String to, String code) {
        logCodeIfNotConfigured(to, "redefinição de senha", code);
        send(to, "Seu código para redefinir a senha",
                codeEmail("Redefinição de senha",
                        "Use o código abaixo para criar uma nova senha. Ele expira em 15 minutos.",
                        code,
                        "Se não foi você que pediu, ignore este e-mail."));
    }

    public void sendEmailVerificationCode(String to, String code) {
        logCodeIfNotConfigured(to, "verificação de e-mail", code);
        send(to, "Confirme seu e-mail",
                codeEmail("Confirmação de e-mail",
                        "Use o código abaixo para confirmar seu e-mail. Ele expira em 15 minutos.",
                        code,
                        "Se não foi você que criou a conta, ignore este e-mail."));
    }

    /** Em dev (sem Resend) o código não sai por e-mail — deixa no log pra dar pra testar. */
    private void logCodeIfNotConfigured(String to, String kind, String code) {
        if (!isConfigured()) {
            log.warn("[EmailService][DEV] Código de {} para {}: {}", kind, to, code);
        }
    }

    private void send(String to, String subject, String html) {
        if (!isConfigured()) {
            log.warn("[EmailService] Resend não configurado. E-mail NÃO enviado para {} — assunto: {}", to, subject);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", from,
                "to", to,
                "subject", subject,
                "html", html
        );

        webClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        r -> {},
                        e -> log.error("[EmailService] Falha ao enviar e-mail para {}: {}", to, e.getMessage())
                );
    }

    private String codeEmail(String title, String intro, String code, String footer) {
        return """
                <div style="font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:480px;margin:0 auto;color:#1B1917">
                  <h2 style="font-weight:600">%s</h2>
                  <p style="color:#4A4441">%s</p>
                  <p style="font-size:32px;letter-spacing:8px;font-weight:700;margin:24px 0">%s</p>
                  <p style="color:#7C7570;font-size:13px">%s</p>
                </div>
                """.formatted(title, intro, code, footer);
    }
}
