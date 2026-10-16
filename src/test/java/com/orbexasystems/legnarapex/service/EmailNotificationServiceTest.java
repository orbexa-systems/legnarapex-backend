package com.orbexasystems.legnarapex.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EmailNotificationServiceTest {

    private MockRestServiceServer server;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new EmailNotificationService(builder);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "from", "onboarding@resend.dev");
        ReflectionTestUtils.setField(service, "notifyTo", "recipient@gmail.com");
    }

    @Test
    void sendCleanupSuccess_withDeletedPhotos_callsResendApi() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.to[0]").value("recipient@gmail.com"))
                .andExpect(jsonPath("$.subject").value(org.hamcrest.Matchers.containsString("5")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("5")))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        service.sendCleanupSuccess(5);

        server.verify();
    }

    @Test
    void sendCleanupSuccess_withZeroPhotos_sendsNoPhotosMessage() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.subject").value(org.hamcrest.Matchers.containsString("sin fotos")))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        service.sendCleanupSuccess(0);

        server.verify();
    }

    @Test
    void sendCleanupFailure_sendsErrorMessage() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.subject").value(org.hamcrest.Matchers.containsString("ERROR")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("DB connection lost")))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        service.sendCleanupFailure("DB connection lost");

        server.verify();
    }

    @Test
    void send_whenApiKeyNotConfigured_skipsRequest() {
        ReflectionTestUtils.setField(service, "apiKey", "");

        service.sendCleanupSuccess(3);

        server.verify(); // no requests expected
    }

    @Test
    void send_whenResendReturnsError_doesNotPropagateException() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withServerError());

        // should not throw
        service.sendCleanupSuccess(2);
    }
}
