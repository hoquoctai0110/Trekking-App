package com.example.trekkingapp.auth;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailOtpNotificationServiceTest {

    @Test
    void devProfileLogsOtpWhenCredentialsAreMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        MailOtpNotificationService service = new MailOtpNotificationService(mailSender, environment, "", "", "");

        assertDoesNotThrow(() -> service.sendOtpEmail(
                "user@example.com",
                "123456",
                AuthOtpPurpose.REGISTER_VERIFY,
                LocalDateTime.now().plusMinutes(10)
        ));
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void productionProfileFailsWhenMailConfigIsMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        MailOtpNotificationService service = new MailOtpNotificationService(mailSender, environment, "", "", "");

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
    void sendsMimeMessageWhenMailConfigIsPresent() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        MailOtpNotificationService service = new MailOtpNotificationService(
                mailSender,
                environment,
                "noreply@chektrek.com",
                "smtp-user",
                "smtp-password"
        );

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendOtpEmail(
                "user@example.com",
                "123456",
                AuthOtpPurpose.REGISTER_VERIFY,
                LocalDateTime.of(2026, 7, 7, 17, 30)
        );

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void usesMailUsernameAsFromAddressFallbackWhenMailFromIsMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        MailOtpNotificationService service = new MailOtpNotificationService(
                mailSender,
                environment,
                "smtp-user@gmail.com",
                "smtp-user@gmail.com",
                "smtp-password"
        );

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> service.sendOtpEmail(
                "user@example.com",
                "123456",
                AuthOtpPurpose.REGISTER_VERIFY,
                LocalDateTime.of(2026, 7, 7, 17, 30)
        ));

        verify(mailSender).send(mimeMessage);
    }
}
