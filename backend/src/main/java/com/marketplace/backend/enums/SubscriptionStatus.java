package com.marketplace.backend.enums;

public enum SubscriptionStatus {
    /** Legado, não usado no fluxo atual. */
    FREE,
    /** Dentro do período gratuito (7, 15 ou 30 dias). Acesso liberado. */
    TRIAL,
    /** Assinatura paga e ativa no Mercado Pago. Acesso liberado. */
    ACTIVE,
    /** Pagamento recorrente falhou; dentro do período de tolerância. Acesso liberado. */
    PAST_DUE,
    /** Período de teste terminou sem o cliente nunca ter assinado. Acesso BLOQUEADO. */
    EXPIRED,
    /** Assinatura cancelada ou tolerância esgotada após falha de pagamento. Acesso BLOQUEADO. */
    SUSPENDED;

    /** Status em que o dono perde acesso ao painel (mas os dados são mantidos). */
    public boolean isBlocked() {
        return this == EXPIRED || this == SUSPENDED;
    }
}
