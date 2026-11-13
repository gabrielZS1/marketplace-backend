package com.marketplace.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Trava de segurança de produção. Só roda com o perfil "prod".
 *
 * O application.properties tem valores padrão de desenvolvimento. Se uma variável de ambiente
 * obrigatória esquecer de ser definida no servidor, o app subiria "funcionando" com um padrão
 * conhecido (público no repositório). Para o JWT isso é grave — qualquer pessoa assinaria
 * tokens válidos —, então o app se recusa a iniciar. Nos demais casos só avisa no log.
 */
@Component
@Profile("prod")
public class ProductionSecurityGuard {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityGuard.class);

    /** 256 bits: mínimo para HS256. (O Render gera valores de 44 caracteres.) */
    private static final int MIN_JWT_SECRET_CHARS = 32;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.admin-email:}")
    private String adminEmail;

    @Value("${app.base-url:}")
    private String baseUrl;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @PostConstruct
    void check() {
        String secret = jwtSecret == null ? "" : jwtSecret.trim();
        if (secret.isEmpty() || secret.startsWith("dev-") || secret.length() < MIN_JWT_SECRET_CHARS) {
            throw new IllegalStateException(
                    "JWT_SECRET inseguro em produção: defina a variável de ambiente JWT_SECRET com um valor "
                            + "aleatório de pelo menos " + MIN_JWT_SECRET_CHARS + " caracteres (o ideal é 64).");
        }
        if (secret.length() < 64) {
            log.warn("JWT_SECRET tem {} caracteres; o recomendado é 64 ou mais.", secret.length());
        }

        if (adminEmail == null || adminEmail.isBlank() || "admin@example.com".equalsIgnoreCase(adminEmail.trim())) {
            log.warn("ADMIN_EMAIL não definido (padrão de desenvolvimento): a geração de códigos promocionais "
                    + "fica bloqueada até você configurar um e-mail de administrador real.");
        }

        if (baseUrl == null || baseUrl.contains("localhost") || baseUrl.contains("192.168.")) {
            log.warn("APP_BASE_URL aponta para um endereço local ({}). Links de e-mail e o retorno do "
                    + "Mercado Pago ficarão errados — defina APP_BASE_URL com o endereço público.", baseUrl);
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("MP_WEBHOOK_SECRET vazio: todos os webhooks do Mercado Pago serão recusados.");
        }
    }
}
