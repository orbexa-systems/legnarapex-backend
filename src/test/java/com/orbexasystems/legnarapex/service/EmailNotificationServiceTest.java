package com.orbexasystems.legnarapex.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationService, "from", "sender@gmail.com");
        ReflectionTestUtils.setField(emailNotificationService, "notifyTo", "recipient@gmail.com");
    }

    @Test
    void sendCleanupSuccess_withDeletedPhotos_sendsEmailWithCount() {
        emailNotificationService.sendCleanupSuccess(5);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("5");
        assertThat(sent.getText()).contains("5");
        assertThat(sent.getTo()).containsExactly("recipient@gmail.com");
    }

    @Test
    void sendCleanupSuccess_withZeroPhotos_sendsEmailWithNoPhotosMessage() {
        emailNotificationService.sendCleanupSuccess(0);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getSubject()).contains("sin fotos");
    }

    @Test
    void sendCleanupFailure_sendsEmailWithErrorMessage() {
        emailNotificationService.sendCleanupFailure("DB connection lost");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("ERROR");
        assertThat(sent.getText()).contains("DB connection lost");
    }

    @Test
    void send_whenCredentialsNotConfigured_skipsEmail() {
        ReflectionTestUtils.setField(emailNotificationService, "from", "");
        ReflectionTestUtils.setField(emailNotificationService, "notifyTo", "");

        emailNotificationService.sendCleanupSuccess(3);

        verifyNoInteractions(mailSender);
    }

    @Test
    void send_whenMailSenderThrows_doesNotPropagateException() {
        doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // should not throw
        emailNotificationService.sendCleanupSuccess(2);
    }
}
