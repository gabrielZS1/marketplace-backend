package com.marketplace.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class PushNotificationService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://exp.host/--/api/v2/push/send")
            .build();

    public void sendAppointmentCreated(String expoPushToken, String clientName, String serviceName) {

        if (expoPushToken == null || expoPushToken.isBlank()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "to", expoPushToken,
                "sound", "default",
                "title", "Novo agendamento!",
                "body", clientName + " agendou " + serviceName,
                "channelId", "agendamentos",
                "priority", "high"
        );

        webClient.post()
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> {},
                        error -> System.err.println("Falha ao enviar push: " + error.getMessage())
                );
    }
}