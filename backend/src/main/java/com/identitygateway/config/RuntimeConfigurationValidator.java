package com.identitygateway.config;

import com.identitygateway.auth.AuthHardeningProperties;
import com.identitygateway.dopa.DopaIntegrationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class RuntimeConfigurationValidator implements ApplicationRunner {

    private final DopaIntegrationProperties dopaProperties;
    private final AuthHardeningProperties authHardeningProperties;
    private final Duration sessionTtl;
    private final String allowedOrigins;

    public RuntimeConfigurationValidator(
            DopaIntegrationProperties dopaProperties,
            AuthHardeningProperties authHardeningProperties,
            @Value("${app.auth.session-ttl:PT8H}") Duration sessionTtl,
            @Value("${app.cors.allowed-origins:}") String allowedOrigins
    ) {
        this.dopaProperties = dopaProperties;
        this.authHardeningProperties = authHardeningProperties;
        this.sessionTtl = sessionTtl;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        List<String> errors = new ArrayList<>();
        validateAuth(errors);
        validateDopa(errors);
        validateCors(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid runtime configuration: " + String.join("; ", errors));
        }
    }

    private void validateAuth(List<String> errors) {
        if (!isPositive(sessionTtl)) {
            errors.add("app.auth.session-ttl must be greater than zero");
        }

        AuthHardeningProperties.Lockout lockout = authHardeningProperties.getLockout();
        if (lockout.isEnabled() && !isPositive(lockout.getDuration())) {
            errors.add("app.auth.hardening.lockout.duration must be greater than zero when lockout is enabled");
        }

        AuthHardeningProperties.PasswordPolicy password = authHardeningProperties.getPassword();
        if (password.getMaxLength() < password.getMinLength()) {
            errors.add("app.auth.hardening.password.max-length must be greater than or equal to min-length");
        }

        AuthHardeningProperties.SessionCleanup cleanup = authHardeningProperties.getSessionCleanup();
        if (cleanup.isEnabled()) {
            if (!isPositive(cleanup.getRetention())) {
                errors.add("app.auth.hardening.session-cleanup.retention must be greater than zero when cleanup is enabled");
            }
            if (!isPositive(cleanup.getFixedDelay())) {
                errors.add("app.auth.hardening.session-cleanup.fixed-delay must be greater than zero when cleanup is enabled");
            }
        }
    }

    private void validateDopa(List<String> errors) {
        if (dopaProperties.getMode() != DopaIntegrationProperties.Mode.PARTNER) {
            return;
        }

        DopaIntegrationProperties.Partner partner = dopaProperties.getPartner();
        if (!StringUtils.hasText(partner.getBaseUrl())) {
            errors.add("app.dopa.partner.base-url is required when app.dopa.mode=PARTNER");
        } else if (!isAbsoluteHttpUri(partner.getBaseUrl())) {
            errors.add("app.dopa.partner.base-url must be an absolute http or https URL");
        }

        if (!StringUtils.hasText(partner.getValidationPath())) {
            errors.add("app.dopa.partner.validation-path is required when app.dopa.mode=PARTNER");
        } else if (!partner.getValidationPath().trim().startsWith("/")) {
            errors.add("app.dopa.partner.validation-path must start with /");
        }

        if (!StringUtils.hasText(partner.getApiKey())) {
            errors.add("app.dopa.partner.api-key is required when app.dopa.mode=PARTNER");
        }
        if (!isPositive(partner.getConnectTimeout())) {
            errors.add("app.dopa.partner.connect-timeout must be greater than zero");
        }
        if (!isPositive(partner.getReadTimeout())) {
            errors.add("app.dopa.partner.read-timeout must be greater than zero");
        }
    }

    private void validateCors(List<String> errors) {
        if (!StringUtils.hasText(allowedOrigins)) {
            errors.add("app.cors.allowed-origins must contain at least one origin");
        }
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }

    private static boolean isAbsoluteHttpUri(String value) {
        try {
            URI uri = URI.create(value.trim());
            return uri.isAbsolute()
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && StringUtils.hasText(uri.getHost());
        } catch (RuntimeException ex) {
            return false;
        }
    }
}