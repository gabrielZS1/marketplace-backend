package com.marketplace.backend.controller;

import com.marketplace.backend.entity.Business;
import com.marketplace.backend.enums.SubscriptionStatus;
import com.marketplace.backend.repository.BusinessRepository;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.resources.preapproval.Preapproval;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final BusinessRepository businessRepository;
    private final PreapprovalClient preapprovalClient = new PreapprovalClient();

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    @Value("${app.subscription.grace-period-days}")
    private int gracePeriodDays;

    public WebhookController(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receive(
            HttpServletRequest request,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId
    ) {
        String dataId = request.getParameter("data.id");
        if (dataId == null) {
            dataId = request.getParameter("id");
        }

        if (!isValidSignature(xSignature, xRequestId, dataId)) {
            return ResponseEntity.status(401).build();
        }

        if (dataId == null) {
            return ResponseEntity.ok().build();
        }

        try {
            Preapproval preapproval = preapprovalClient.get(dataId);
            applySubscriptionUpdate(preapproval);
        } catch (Exception e) {
            // Loga o erro mas retorna 200 pra evitar reenvio infinito do MP em erros de leitura nossa;
            // se preferir reprocessamento automático pelo MP, retorne 500 aqui.
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.ok().build();
    }

    private void applySubscriptionUpdate(Preapproval preapproval) {
        String externalReference = preapproval.getExternalReference();
        if (externalReference == null) return;

        UUID businessId;
        try {
            businessId = UUID.fromString(externalReference);
        } catch (IllegalArgumentException e) {
            return;
        }

        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) return;

        // Idempotência: se o preapproval salvo não bate com o que veio no webhook
        // (ex: notificação atrasada de uma assinatura já substituída num upgrade), ignora.
        if (business.getMpPreapprovalId() != null
                && !business.getMpPreapprovalId().equals(preapproval.getId())) {
            return;
        }

        String status = preapproval.getStatus(); // authorized, paused, cancelled, pending

        switch (status) {
            case "authorized" -> {
                business.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                business.setSubscriptionGraceEndsAt(null);
                business.setActive(true);
                business.setCurrentPeriodEnd(OffsetDateTime.now().plusMonths(1));
            }
            case "paused" -> {
                if (business.getSubscriptionStatus() != SubscriptionStatus.PAST_DUE) {
                    business.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
                    business.setSubscriptionGraceEndsAt(OffsetDateTime.now().plusDays(gracePeriodDays));
                }
            }
            case "cancelled" -> {
                business.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
                business.setActive(false);
            }
            default -> { /* pending: não faz nada, aguarda próxima notificação */ }
        }

        businessRepository.save(business);
    }

    private boolean isValidSignature(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || dataId == null) return false;

        String ts = null;
        String hash = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            if (key.equals("ts")) ts = value;
            if (key.equals("v1")) hash = value;
        }

        if (ts == null || hash == null) return false;

        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + xRequestId + ";ts:" + ts + ";";

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : computed) hex.append(String.format("%02x", b));

            return java.security.MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    hash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}