package com.marketplace.backend.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoSetup {

    private final String accessToken;

    public MercadoPagoSetup(@Value("${mercadopago.access-token}") String accessToken) {
        this.accessToken = accessToken;
    }

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}