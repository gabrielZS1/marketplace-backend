package com.marketplace.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class PushNotificationService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://exp.host/--/api/v2/push/send")
            .build();

    // =========================================================
    // NOTIFICA O ESTABELECIMENTO (dono/admin) quando um cliente
    // cria um novo agendamento
    // =========================================================

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

    // =========================================================
    // NOTIFICA O CLIENTE quando o estabelecimento confirma ou
    // recusa (cancela) o agendamento
    // =========================================================

    public void sendAppointmentStatusChanged(String expoPushToken, String businessName, String status) {

        if (expoPushToken == null || expoPushToken.isBlank()) {
            return;
        }

        String title;
        String body;

        if ("CONFIRMED".equals(status)) {
            title = "Agendamento confirmado!";
            body = businessName + " confirmou o seu horário.";
        } else if ("CANCELLED".equals(status)) {
            title = "Agendamento recusado";
            body = businessName + " não pôde confirmar o seu horário.";
        } else {
            return; // outros status não geram notificação por enquanto
        }

        Map<String, Object> payload = Map.of(
                "to", expoPushToken,
                "sound", "default",
                "title", title,
                "body", body,
                "channelId", "agendamentos",
                "priority", "high"
        );

        webClient.post()
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> {},
                        error -> System.err.println("Falha ao enviar push pro cliente: " + error.getMessage())
                );
    }
}