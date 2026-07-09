package com.example.trekkingapp.auth;

import com.example.trekkingapp.common.RequestTracing;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MailOtpNotificationService implements OtpNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailOtpNotificationService.class);
    private static final String APP_NAME = "ChekTrek";
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final Environment environment;
    private final String fromAddress;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailHost;
    private final String mailPort;

    public MailOtpNotificationService(
            JavaMailSender mailSender,
            Environment environment,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.port:}") String mailPort
    ) {
        this.mailSender = mailSender;
        this.environment = environment;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.mailUsername = mailUsername == null ? "" : mailUsername.trim();
        this.mailPassword = mailPassword == null ? "" : mailPassword.trim();
        this.mailHost = mailHost == null ? "" : mailHost.trim();
        this.mailPort = mailPort == null ? "" : mailPort.trim();
    }

    @Override
    public void sendOtpEmail(String email, String otp, AuthOtpPurpose purpose, LocalDateTime expiresAt) {
        String requestId = RequestTracing.getRequestId();
        if (shouldLogOtpOnly()) {
            log.info("[MAIL][{}] DEV_MODE_SKIP_SEND to={} purpose={} expiry={}", requestId, email, purpose, expiresAt);
            return;
        }

        ensureMailConfigured();

        try {
            long startedAt = System.nanoTime();
            log.info("[MAIL][{}] PREPARE_MESSAGE", requestId);
            log.info("[MAIL][{}] SMTP_HOST={}", requestId, mailHost);
            log.info("[MAIL][{}] SMTP_PORT={}", requestId, mailPort);
            log.info("[MAIL][{}] FROM={}", requestId, fromAddress);
            log.info("[MAIL][{}] TO={}", requestId, email);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(email);
            helper.setFrom(fromAddress);
            helper.setSubject(buildSubject(purpose));
            helper.setText(buildHtmlBody(otp, purpose, expiresAt), true);
            log.info("[MAIL][{}] BEFORE_JAVA_MAIL_SEND", requestId);
            mailSender.send(message);
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("[MAIL][{}] AFTER_JAVA_MAIL_SEND duration={}ms", requestId, durationMs);
        } catch (MessagingException | MailException exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            log.error("[MAIL][{}] SEND_FAILED exceptionClass={} message={} rootCauseClass={} rootCauseMessage={}",
                    requestId,
                    exception.getClass().getName(),
                    exception.getMessage(),
                    rootCause.getClass().getName(),
                    rootCause.getMessage(),
                    exception);
            throw new EmailDeliveryException("Failed to send OTP email. Please try again later.", exception);
        }
    }

    private boolean shouldLogOtpOnly() {
        return isDevelopmentProfile() && (mailUsername.isBlank() || mailPassword.isBlank());
    }

    private boolean isDevelopmentProfile() {
        return environment.acceptsProfiles(Profiles.of("dev", "local"));
    }

    private void ensureMailConfigured() {
        if (fromAddress.isBlank()) {
            throw new EmailDeliveryException("Email delivery is not configured: MAIL_FROM or MAIL_USERNAME is required.");
        }

        if (mailUsername.isBlank() || mailPassword.isBlank()) {
            throw new EmailDeliveryException("Email delivery is not configured: MAIL_USERNAME and MAIL_PASSWORD are required.");
        }
    }

    private String buildSubject(AuthOtpPurpose purpose) {
        return APP_NAME + " " + purposeLabel(purpose) + " OTP";
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

    private String purposeLabel(AuthOtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER_VERIFY -> "Account Verification";
            case PASSWORD_RESET -> "Password Reset";
        };
    }

    private String purposeDescription(AuthOtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER_VERIFY -> "account verification";
            case PASSWORD_RESET -> "password reset";
        };
    }
}
