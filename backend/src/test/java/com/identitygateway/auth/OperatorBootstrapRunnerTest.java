package com.identitygateway.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorBootstrapRunnerTest {

    @Mock
    private OperatorUserRepository operatorUserRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void runCreatesOperatorWhenBootstrapIsEnabled() {
        OperatorBootstrapRunner runner = new OperatorBootstrapRunner(
                operatorUserRepository,
                passwordEncoder,
                true,
                "operator",
                "StrongPassword123!",
                "Operations User",
                OperatorRole.OPERATIONS
        );
        when(operatorUserRepository.existsByUsernameIgnoreCase("operator")).thenReturn(false);

        runner.run(new DefaultApplicationArguments());

        ArgumentCaptor<OperatorUser> userCaptor = ArgumentCaptor.forClass(OperatorUser.class);
        verify(operatorUserRepository).save(userCaptor.capture());
        OperatorUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("operator");
        assertThat(savedUser.getDisplayName()).isEqualTo("Operations User");
        assertThat(savedUser.getRole()).isEqualTo(OperatorRole.OPERATIONS);
        assertThat(passwordEncoder.matches("StrongPassword123!", savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getPasswordHash()).doesNotContain("StrongPassword123!");
    }

    @Test
    void runSkipsCreationWhenBootstrapIsDisabled() {
        OperatorBootstrapRunner runner = new OperatorBootstrapRunner(
                operatorUserRepository,
                passwordEncoder,
                false,
                "operator",
                "StrongPassword123!",
                "Operations User",
                OperatorRole.OPERATIONS
        );

        runner.run(new DefaultApplicationArguments());

        verify(operatorUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runRejectsBlankPasswordWhenBootstrapIsEnabled() {
        OperatorBootstrapRunner runner = new OperatorBootstrapRunner(
                operatorUserRepository,
                passwordEncoder,
                true,
                "operator",
                "",
                "Operations User",
                OperatorRole.OPERATIONS
        );

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bootstrap operator password is required.");
    }
}