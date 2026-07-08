package com.example.trekkingapp.config;

import com.example.trekkingapp.auth.GoogleProperties;
import com.example.trekkingapp.payment.PayOSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

@Component
public class StartupConfigurationLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigurationLogger.class);

    private final Environment environment;
    private final GoogleProperties googleProperties;
    private final PayOSProperties payOSProperties;

    public StartupConfigurationLogger(
            Environment environment,
            GoogleProperties googleProperties,
            PayOSProperties payOSProperties
    ) {
        this.environment = environment;
        this.googleProperties = googleProperties;
        this.payOSProperties = payOSProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("startup_active_profiles={}", activeProfiles.length == 0 ? "[default]" : Arrays.toString(activeProfiles));
        log.info("startup_google_mock_enabled={}", googleProperties.isMockEnabled());
        log.info("startup_payos_return_url={}", maskUrl(payOSProperties.getReturnUrl()));
        log.info("startup_payos_cancel_url={}", maskUrl(payOSProperties.getCancelUrl()));
        log.info("startup_datasource={}", maskJdbcUrl(environment.getProperty("spring.datasource.url")));
    }

    private String maskUrl(String value) {
        if (value == null || value.isBlank()) {
            return "[not-set]";
        }

        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "[invalid-url]";
            }
            return uri.getScheme() + "://" + host;
        } catch (RuntimeException exception) {
            return "[invalid-url]";
        }
    }

    private String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "[not-set]";
        }

        String normalized = jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring(5) : jdbcUrl;
        try {
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "[invalid-jdbc-url]";
            }
            return "jdbc:" + uri.getScheme() + "://" + host;
        } catch (RuntimeException exception) {
            return "[invalid-jdbc-url]";
        }
    }
}
