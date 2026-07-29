package com.identitygateway.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyServiceTest {

    @Test
    void validateAcceptsPasswordThatMatchesConfiguredPolicy() {
        PasswordPolicyService service = new PasswordPolicyService(new AuthHardeningProperties());

        assertThatCode(() -> service.validate("Valid-password-123"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRejectsPasswordMissingRequiredCharacterClass() {
        PasswordPolicyService service = new PasswordPolicyService(new AuthHardeningProperties());

        assertThatThrownBy(() -> service.validate("invalid-password-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must include an uppercase letter.");
    }

    @Test
    void validateUsesConfigurableSpecialCharacterRequirement() {
        AuthHardeningProperties properties = new AuthHardeningProperties();
        properties.getPassword().setRequireSpecial(true);
        PasswordPolicyService service = new PasswordPolicyService(properties);

        assertThatThrownBy(() -> service.validate("Validpassword123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must include a special character.");
    }
}