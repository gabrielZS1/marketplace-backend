package com.marketplace.backend.service;

import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preapproval.Preapproval;
import com.marketplace.backend.entity.Business;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SubscriptionService {

    private final PreapprovalClient preapprovalClient = new PreapprovalClient();

    @Value("${app.base-url}")
    private String baseUrl;

    public SubscriptionResult createSubscription(Business business) {
        String payerEmail = business.getOwner().getEmail();

        PreApprovalAutoRecurringCreateRequest autoRecurring =
                PreApprovalAutoRecurringCreateRequest.builder()
                        .frequency(1)
                        .frequencyType("months")
                        .transactionAmount(business.getPlanPrice())
                        .currencyId("BRL")
                        .build();

        PreapprovalCreateRequest request = PreapprovalCreateRequest.builder()
                .reason("Assinatura " + business.getName() + " - " + business.getTeamSize() + " profissional(is)")
                .externalReference(business.getId().toString())
                .payerEmail(payerEmail)
                .backUrl(baseUrl + "/api/businesses/onboarding/" + business.getId() + "/subscription-callback")
                .autoRecurring(autoRecurring)
                .status("pending")
                .build();

        try {
            Preapproval preapproval = preapprovalClient.create(request);
            return new SubscriptionResult(preapproval.getId(), preapproval.getInitPoint(), preapproval.getStatus());
        } catch (MPApiException e) {
            throw new RuntimeException("Erro ao criar assinatura no Mercado Pago: " + e.getApiResponse().getContent(), e);
        } catch (MPException e) {
            throw new RuntimeException("Erro ao comunicar com o Mercado Pago", e);
        }
    }

    public void cancelSubscription(String preapprovalId) {
        if (preapprovalId == null) return;
        try {
            com.mercadopago.client.preapproval.PreapprovalUpdateRequest cancelRequest =
                    com.mercadopago.client.preapproval.PreapprovalUpdateRequest.builder()
                            .status("cancelled")
                            .build();
            preapprovalClient.update(preapprovalId, cancelRequest);
        } catch (MPApiException e) {
            throw new RuntimeException("Erro ao cancelar assinatura no Mercado Pago: " + e.getApiResponse().getContent(), e);
        } catch (MPException e) {
            throw new RuntimeException("Erro ao comunicar com o Mercado Pago", e);
        }
    }

    public record SubscriptionResult(String preapprovalId, String paymentUrl, String status) {}
}