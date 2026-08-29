package com.marketplace.backend.config;

import com.marketplace.backend.security.RateLimitFilter;
import com.marketplace.backend.security.RateLimiter;
import com.marketplace.backend.security.UploadsHeaderFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    /**
     * Registra o rate limiter com a maior precedência possível, para rodar
     * antes da cadeia do Spring Security.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimiter rateLimiter,
            @Value("${app.ratelimit.enabled:true}") boolean enabled,
            @Value("${app.ratelimit.behind-proxy:false}") boolean behindProxy
    ) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(rateLimiter, enabled, behindProxy));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /** Força download (nunca renderização) dos arquivos em /uploads/**. */
    @Bean
    public FilterRegistrationBean<UploadsHeaderFilter> uploadsHeaderFilter() {
        FilterRegistrationBean<UploadsHeaderFilter> registration =
                new FilterRegistrationBean<>(new UploadsHeaderFilter());
        registration.addUrlPatterns("/uploads/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
