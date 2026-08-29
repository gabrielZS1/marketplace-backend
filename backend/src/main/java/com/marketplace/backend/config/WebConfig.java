package com.marketplace.backend.config;

import com.marketplace.backend.security.SubscriptionAccessInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final SubscriptionAccessInterceptor subscriptionAccessInterceptor;

    public WebConfig(SubscriptionAccessInterceptor subscriptionAccessInterceptor) {
        this.subscriptionAccessInterceptor = subscriptionAccessInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = new File(uploadDir).getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + File.separator);
    }

    // URLs limpas dos documentos legais (sem .html) — para lojas e links do app.
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/legal/termos").setViewName("forward:/legal/termos.html");
        registry.addViewController("/legal/privacidade").setViewName("forward:/legal/privacidade.html");
        registry.addViewController("/legal").setViewName("forward:/legal/termos.html");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(subscriptionAccessInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/webhooks/**");
    }
}
