package com.orbexasystems.legnarapex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailNotificationService {

    private final RestClient restClient;

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String from;

    @Value("${mail.notify-to:}")
    private String notifyTo;

    public EmailNotificationService(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.resend.com").build();
    }

    public void sendCleanupSuccess(int deletedCount) {
        String subject = deletedCount > 0
                ? String.format("Legnarapex ✅ Limpieza: %d foto(s) eliminada(s)", deletedCount)
                : "Legnarapex ✅ Limpieza ejecutada — sin fotos que borrar";

        String body = deletedCount > 0
                ? String.format("La limpieza diaria se ejecutó correctamente.\n\nFotos eliminadas: %d", deletedCount)
                : "La limpieza diaria se ejecutó correctamente.\n\nNo había fotos almacenadas al momento de la limpieza.";

        send(subject, body);
    }

    public void sendCleanupFailure(String errorMessage) {
        send(
            "Legnarapex ❌ ERROR en la limpieza diaria",
            String.format("La limpieza diaria falló y no se eliminaron fotos.\n\nError:\n%s\n\nRevisa los logs en Railway.", errorMessage)
        );
    }

    private void send(String subject, String body) {
        if (apiKey.isBlank() || notifyTo.isBlank()) {
            log.warn("Resend not configured — skipping email notification");
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(notifyTo),
                "subject", subject,
                "text", body
            );
            restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
            log.info("Cleanup notification sent to {}", notifyTo);
        } catch (Exception e) {
            log.error("Failed to send cleanup notification email", e);
        }
    }
}
