package com.example.trekkingapp.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResendEmailServiceTest {

    @Test
    void devProfileLogsOtpWhenApiKeyIsMissing() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        ResendEmailService service = new ResendEmailService(builder, environment, "noreply@chektrek.com", "");

        assertDoesNotThrow(() -> service.sendOtpEmail(
                "user@example.com",
                "123456",
                AuthOtpPurpose.REGISTER_VERIFY,
                LocalDateTime.now().plusMinutes(10)
        ));
        verify(builder).build();
    }

    @Test
    void productionProfileFailsWhenApiKeyIsMissing() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ResendEmailService service = new ResendEmailService(builder, environment, "noreply@chektrek.com", "");

        assertThrows(
                EmailDeliveryException.class,
                () -> service.sendOtpEmail(
                        "user@example.com",
                        "123456",
                        AuthOtpPurpose.PASSWORD_RESET,
                        LocalDateTime.now().plusMinutes(10)
                )
        );
    }

    @Test
    void productionProfileFailsWhenMailFromIsMissing() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ResendEmailService service = new ResendEmailService(builder, environment, "", "re_test_api_key");

        assertThrows(
                EmailDeliveryException.class,
                () -> service.sendOtpEmail(
                        "user@example.com",
                        "123456",
                        AuthOtpPurpose.PASSWORD_RESET,
                        LocalDateTime.now().plusMinutes(10)
                )
        );
    }

    @Test
    void wrapsResendApiErrors() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenThrow(new RestClientException("timeout"));

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ResendEmailService service = new ResendEmailService(builder, environment, "noreply@chektrek.com", "re_test_api_key");

        assertThrows(
                EmailDeliveryException.class,
                () -> service.sendOtpEmail(
                        "user@example.com",
                        "123456",
                        AuthOtpPurpose.REGISTER_VERIFY,
                        LocalDateTime.now().plusMinutes(10)
                )
        );
    }
}
