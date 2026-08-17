package com.marketplace.backend.service;

import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalUpdateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.marketplace.backend.entity.Business;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SubscriptionService {

    private final PreapprovalClient preapprovalClient = new PreapprovalClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${app.base-url}")
    private String baseUrl;

    public SubscriptionResult createSubscription(Business business, int freeMonths) {
        String payerEmail = business.getOwner().getEmail();

        Map<String, Object> autoRecurring = new LinkedHashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", business.getPlanPrice());
        autoRecurring.put("currency_id", "BRL");

        if (freeMonths > 0) {
            Map<String, Object> freeTrial = new LinkedHashMap<>();
            freeTrial.put("frequency", freeMonths);
            freeTrial.put("frequency_type", "months");
            autoRecurring.put("free_trial", freeTrial);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", "Assinatura " + business.getName() + " - " + business.getTeamSize() + " profissional(is)");
        body.put("external_reference", business.getId().toString());
        body.put("payer_email", payerEmail);
        body.put("back_url", baseUrl + "/api/businesses/onboarding/" + business.getId() + "/subscription-callback");
        body.put("auto_recurring", autoRecurring);
        body.put("status", "pending");

        try {
            String json = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/preapproval"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Erro ao criar assinatura no Mercado Pago: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            return new SubscriptionResult(
                    node.get("id").asText(),
                    node.get("init_point").asText(),
                    node.get("status").asText()
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com o Mercado Pago", e);
        }
    }

    public void cancelSubscription(String preapprovalId) {
        if (preapprovalId == null) return;
        try {
            PreapprovalUpdateRequest cancelRequest = PreapprovalUpdateRequest.builder()
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