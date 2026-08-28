package com.marketplace.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Aplica limites de requisição por IP. Roda antes de tudo (inclusive antes da
 * autenticação), então protege o /auth/login contra força bruta e barra floods
 * genéricos antes de gastar CPU com JWT/DB.
 *
 * Registrado em {@link com.marketplace.backend.config.FilterConfig}.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** regra: método (ou null = qualquer), prefixo do path, capacidade, reposição/janela, janela(s) */
    private record Rule(String method, String pathPrefix, int capacity, int refill, int windowSeconds) {
        boolean matches(String method, String path) {
            return (this.method == null || this.method.equalsIgnoreCase(method))
                    && path.startsWith(this.pathPrefix);
        }
    }

    // Ordem importa: a primeira regra que casar é a usada.
    // Limites por IP. Números folgados de propósito: muitos celulares saem pelo
    // mesmo IP (CGNAT da operadora), então limite apertado derrubaria gente de boa.
    // A borda (Cloudflare/WAF) é quem deve fazer o corte fino.
    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/auth/login", 30, 30, 300),
            new Rule("POST", "/api/auth/register", 12, 12, 600),
            new Rule("POST", "/api/auth/refresh", 60, 60, 300),
            new Rule(null,   "/api/auth/", 60, 60, 300),
            new Rule("POST", "/api/promo-codes/generate", 20, 20, 300),
            new Rule("POST", "/api/appointments", 30, 30, 300),
            new Rule(null,   "/api/", 300, 300, 60)
    );

    private final RateLimiter rateLimiter;
    private final boolean enabled;
    private final boolean behindProxy;

    public RateLimitFilter(RateLimiter rateLimiter, boolean enabled, boolean behindProxy) {
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
        this.behindProxy = behindProxy;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Webhooks (Mercado Pago) são validados por assinatura HMAC; não limitar.
        if (!enabled || !path.startsWith("/api/") || path.startsWith("/api/webhooks/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Rule rule = null;
        for (Rule r : RULES) {
            if (r.matches(request.getMethod(), path)) {
                rule = r;
                break;
            }
        }

        if (rule != null) {
            String key = rule.pathPrefix() + "|" + clientIp(request);
            if (!rateLimiter.allow(key, rule.capacity(), rule.refill(), rule.windowSeconds())) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(rule.windowSeconds()));
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                        "{\"error\":\"RATE_LIMITED\",\"status\":429,"
                        + "\"message\":\"Muitas tentativas. Aguarde um momento e tente de novo.\"}"
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // Só confia nesses cabeçalhos quando o app está atrás de um proxy que os
        // reescreve (Cloudflare/nginx). Sem isso, um cliente poderia forjá-los.
        if (behindProxy) {
            String cf = request.getHeader("CF-Connecting-IP");
            if (cf != null && !cf.isBlank()) return cf.trim();

            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
