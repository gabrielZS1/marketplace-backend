package com.marketplace.backend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Fonte única de verdade do preço da mensalidade.
 *
 * A cobrança é feita de acordo com a quantidade de profissionais (teamSize):
 *   1 profissional  -> R$ 10,00
 *   2 profissionais -> R$ 20,00
 *   3 profissionais -> R$ 30,00
 *   4 profissionais -> R$ 40,00
 *   5 ou mais       -> R$ 50,00
 */
@Service
public class PricingService {

    public BigDecimal monthlyPriceFor(int teamSize) {
        int tier = Math.max(1, Math.min(teamSize, 5));
        return switch (tier) {
            case 1 -> new BigDecimal("10.00");
            case 2 -> new BigDecimal("20.00");
            case 3 -> new BigDecimal("30.00");
            case 4 -> new BigDecimal("40.00");
            default -> new BigDecimal("50.00");
        };
    }
}
