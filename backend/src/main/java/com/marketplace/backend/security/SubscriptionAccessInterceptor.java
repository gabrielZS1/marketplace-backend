package com.marketplace.backend.security;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.service.BusinessAccessService;
import com.marketplace.backend.service.SubscriptionStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Corta o acesso do DONO ao painel quando a assinatura está bloqueada
 * (trial expirado sem assinar, ou pagamento suspenso).
 *
 * Não apaga nada: os dados continuam no banco. Quando o cliente assinar
 * (webhook do Mercado Pago -> ACTIVE), o acesso volta automaticamente.
 *
 * Rotas sempre liberadas (pra o app conseguir mostrar a tela de renovação):
 *   - /api/auth/**
 *   - GET  /api/businesses/me            (status da assinatura)
 *   - GET  /api/businesses/mine/status
 *   - PATCH /api/businesses/me/push-token
 *   - POST /api/businesses/{id}/subscribe (gera o link de pagamento)
 */
@Component
public class SubscriptionAccessInterceptor implements HandlerInterceptor {

    private static final Pattern SUBSCRIBE_PATH =
            Pattern.compile("^/api/businesses/[^/]+/subscribe/?$");

    private final BusinessAccessService businessAccessService;
    private final SubscriptionStatusService subscriptionStatusService;

    public SubscriptionAccessInterceptor(BusinessAccessService businessAccessService,
                                         SubscriptionStatusService subscriptionStatusService) {
        this.businessAccessService = businessAccessService;
        this.subscriptionStatusService = subscriptionStatusService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !isBusinessMember(auth)) {
            return true;
        }

        if (isAlwaysAllowed(request)) {
            return true;
        }

        UUID userId;
        try {
            userId = UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return true;
        }

        Optional<Business> businessOpt = businessAccessService.resolveBusiness(userId);
        if (businessOpt.isEmpty()) {
            return true;
        }

        Business business = businessOpt.get();

        // Ainda montando o cadastro: libera pra ele terminar (e aplicar um código promo).
        if (!Boolean.TRUE.equals(business.getOnboardingCompleted())) {
            return true;
        }

        SubscriptionStatus effective = subscriptionStatusService.effectiveStatus(business);
        if (!effective.isBlocked()) {
            return true;
        }

        String reason = subscriptionStatusService.blockReason(effective);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"SUBSCRIPTION_BLOCKED\",\"status\":403,\"reason\":\"" + reason + "\"}"
        );
        return false;
    }

    private boolean isBusinessMember(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            String r = a.getAuthority();
            if ("ROLE_BUSINESS_OWNER".equals(r) || "ROLE_EMPLOYEE".equals(r)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlwaysAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/api/auth/")) return true;
        if (path.startsWith("/uploads/")) return true;

        if (path.equals("/api/businesses/me") && method.equals("GET")) return true;
        if (path.equals("/api/businesses/me/push-token") && method.equals("PATCH")) return true;
        if (path.equals("/api/businesses/mine/status") && method.equals("GET")) return true;

        if (SUBSCRIBE_PATH.matcher(path).matches() && method.equals("POST")) return true;

        return false;
    }
}
