package com.identitygateway.config;

import com.identitygateway.auth.AuthHardeningProperties;
import com.identitygateway.dopa.DopaIntegrationProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeConfigurationValidatorTest {

    @Test
    void validateAcceptsDefaultLocalConfiguration() {
        RuntimeConfigurationValidator validator = new RuntimeConfigurationValidator(
                new DopaIntegrationProperties(),
                new AuthHardeningProperties(),
                Duration.ofHours(8),
                "http://127.0.0.1:7000"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void validateRequiresPartnerConfigurationWhenDopaPartnerModeIsEnabled() {
        DopaIntegrationProperties dopa = new DopaIntegrationProperties();
        dopa.setMode(DopaIntegrationProperties.Mode.PARTNER);

        RuntimeConfigurationValidator validator = new RuntimeConfigurationValidator(
                dopa,
                new AuthHardeningProperties(),
                Duration.ofHours(8),
                "http://127.0.0.1:7000"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.dopa.partner.base-url is required")
                .hasMessageContaining("app.dopa.partner.api-key is required");
    }

    @Test
    void validateRejectsInvalidPartnerUrlAndPath() {
        DopaIntegrationProperties dopa = new DopaIntegrationProperties();
        dopa.setMode(DopaIntegrationProperties.Mode.PARTNER);
        dopa.getPartner().setBaseUrl("localhost:8081");
        dopa.getPartner().setValidationPath("validate");
        dopa.getPartner().setApiKey("secret");

        RuntimeConfigurationValidator validator = new RuntimeConfigurationValidator(
                dopa,
                new AuthHardeningProperties(),
                Duration.ofHours(8),
                "http://127.0.0.1:7000"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.dopa.partner.base-url must be an absolute http or https URL")
                .hasMessageContaining("app.dopa.partner.validation-path must start with /");
    }

    @Test
    void validateRejectsInvalidAuthDurations() {
        AuthHardeningProperties auth = new AuthHardeningProperties();
        auth.getLockout().setDuration(Duration.ZERO);
        auth.getSessionCleanup().setRetention(Duration.ZERO);
        auth.getSessionCleanup().setFixedDelay(Duration.ZERO);

        RuntimeConfigurationValidator validator = new RuntimeConfigurationValidator(
                new DopaIntegrationProperties(),
                auth,
                Duration.ZERO,
                "http://127.0.0.1:7000"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.auth.session-ttl must be greater than zero")
                .hasMessageContaining("app.auth.hardening.lockout.duration must be greater than zero")
                .hasMessageContaining("app.auth.hardening.session-cleanup.retention must be greater than zero")
                .hasMessageContaining("app.auth.hardening.session-cleanup.fixed-delay must be greater than zero");
    }

    @Test
    void validateRejectsPasswordPolicyWithMaxLengthBelowMinLength() {
        AuthHardeningProperties auth = new AuthHardeningProperties();
        auth.getPassword().setMinLength(16);
        auth.getPassword().setMaxLength(12);

        RuntimeConfigurationValidator validator = new RuntimeConfigurationValidator(
                new DopaIntegrationProperties(),
                auth,
                Duration.ofHours(8),
                "http://127.0.0.1:7000"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.auth.hardening.password.max-length must be greater than or equal to min-length");
    }

    @Test
    void validateRejectsBlankCorsOrigins() {
        RuntimeConfigurationValidator validator = new RuntimeConfigurationValidator(
                new DopaIntegrationProperties(),
                new AuthHardeningProperties(),
                Duration.ofHours(8),
                " "
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins must contain at least one origin");
    }
}