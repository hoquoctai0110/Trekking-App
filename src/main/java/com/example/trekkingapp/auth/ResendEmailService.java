package com.example.trekkingapp.auth;

import com.example.trekkingapp.common.RequestTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ResendEmailService implements OtpNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String APP_NAME = "ChekTrek";
    private static final String RESEND_BASE_URL = "https://api.resend.com";
    private static final String RESEND_EMAILS_PATH = "/emails";
    private static final int TIMEOUT_MILLIS = 5_000;
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final Environment environment;
    private final String fromAddress;
    private final String resendApiKey;

    public ResendEmailService(
            RestClient.Builder restClientBuilder,
            Environment environment,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${resend.api-key:}") String resendApiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);

        this.restClient = restClientBuilder
                .baseUrl(RESEND_BASE_URL)
                .requestFactory(requestFactory)
                .build();
        this.environment = environment;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
    }

    @Override
    public void sendOtpEmail(String email, String otp, AuthOtpPurpose purpose, LocalDateTime expiresAt) {
        String requestId = RequestTracing.getRequestId();
        if (shouldLogOtpOnly()) {
            log.info("[MAIL][{}] DEV_MODE_SKIP_SEND to={} purpose={} expiry={} otp={}",
                    requestId,
                    email,
                    purpose,
                    expiresAt,
                    otp);
            return;
        }

        ensureMailConfigured();

        try {
            long startedAt = System.nanoTime();
            log.info("[MAIL][{}] PREPARE_MESSAGE", requestId);
            log.info("[MAIL][{}] PROVIDER=RESEND", requestId);
            log.info("[MAIL][{}] FROM={}", requestId, fromAddress);
            log.info("[MAIL][{}] TO={}", requestId, email);

            ResendEmailRequest request = new ResendEmailRequest(
                    fromAddress,
                    List.of(email),
                    buildSubject(purpose),
                    buildHtmlBody(otp, purpose, expiresAt),
                    buildTextBody(otp, purpose, expiresAt)
            );

            log.info("[MAIL][{}] BEFORE_RESEND_API_SEND", requestId);
            ResendEmailResponse response = restClient.post()
                    .uri(RESEND_EMAILS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ResendEmailResponse.class);

            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new EmailDeliveryException("Resend returned an empty response while sending OTP email.");
            }

            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("[MAIL][{}] AFTER_RESEND_API_SEND duration={}ms responseId={}",
                    requestId,
                    durationMs,
                    response.id());
        } catch (RestClientException exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            log.error("[MAIL][{}] SEND_FAILED provider=RESEND exceptionClass={} message={} rootCauseClass={} rootCauseMessage={}",
                    requestId,
                    exception.getClass().getName(),
                    exception.getMessage(),
                    rootCause.getClass().getName(),
                    rootCause.getMessage(),
                    exception);
            throw new EmailDeliveryException("Failed to send OTP email via Resend. Please try again later.", exception);
        } catch (RuntimeException exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            log.error("[MAIL][{}] SEND_FAILED provider=RESEND exceptionClass={} message={} rootCauseClass={} rootCauseMessage={}",
                    requestId,
                    exception.getClass().getName(),
                    exception.getMessage(),
                    rootCause.getClass().getName(),
                    rootCause.getMessage(),
                    exception);
            throw new EmailDeliveryException("Failed to send OTP email via Resend. Please try again later.", exception);
        }
    }

    private boolean shouldLogOtpOnly() {
        return isDevelopmentProfile() && resendApiKey.isBlank();
    }

    private boolean isDevelopmentProfile() {
        return environment.acceptsProfiles(Profiles.of("dev", "local"));
    }

    private void ensureMailConfigured() {
        if (fromAddress.isBlank()) {
            throw new EmailDeliveryException("Email delivery is not configured: MAIL_FROM is required.");
        }

        if (resendApiKey.isBlank()) {
            throw new EmailDeliveryException("Email delivery is not configured: RESEND_API_KEY is required.");
        }
    }

    private String buildSubject(AuthOtpPurpose purpose) {
        return APP_NAME + " " + purposeLabel(purpose) + " OTP verification";
    }

    private String buildHtmlBody(String otp, AuthOtpPurpose purpose, LocalDateTime expiresAt) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #1f2937; line-height: 1.6;">
                  <h2 style="margin-bottom: 8px;">ChekTrek</h2>
                  <p>Your one-time password for %s is:</p>
                  <p style="font-size: 28px; font-weight: bold; letter-spacing: 4px;">%s</p>
                  <p>This code expires at %s.</p>
                  <p>Do not share this OTP with anyone.</p>
                </body>
                </html>
                """.formatted(
                purposeDescription(purpose),
                otp,
                expiresAt.format(EXPIRY_FORMATTER)
        );
    }

    private String buildTextBody(String otp, AuthOtpPurpose purpose, LocalDateTime expiresAt) {
        return """
                ChekTrek

                Your one-time password for %s is: %s
                This code expires at %s.
                Do not share this OTP with anyone.
                """.formatted(
                purposeDescription(purpose),
                otp,
                expiresAt.format(EXPIRY_FORMATTER)
        );
    }

    private String purposeLabel(AuthOtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER_VERIFY -> "Account";
            case PASSWORD_RESET -> "Password Reset";
        };
    }

    private String purposeDescription(AuthOtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER_VERIFY -> "account verification";
            case PASSWORD_RESET -> "password reset";
        };
    }

    private record ResendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String html,
            String text
    ) {
    }

    private record ResendEmailResponse(String id) {
    }
}
