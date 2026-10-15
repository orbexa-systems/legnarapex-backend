package com.orbexasystems.legnarapex.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${mail.notify-to}")
    private String notifyTo;

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
        String subject = "Legnarapex ❌ ERROR en la limpieza diaria";
        String body = String.format(
                "La limpieza diaria falló y no se eliminaron fotos.\n\nError:\n%s\n\nRevisa los logs en Railway.",
                errorMessage
        );
        send(subject, body);
    }

    private void send(String subject, String body) {
        if (from == null || from.isBlank() || notifyTo == null || notifyTo.isBlank()) {
            log.warn("Mail credentials not configured — skipping email notification");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(notifyTo);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Cleanup notification sent to {}", notifyTo);
        } catch (MailException e) {
            log.error("Failed to send cleanup notification email", e);
        }
    }
}
