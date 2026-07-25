package com.identitygateway.auth;

import com.identitygateway.common.error.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OperatorUserRepository operatorUserRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(operatorUserRepository, passwordEncoder);
    }

    @Test
    void loginAcceptsBcryptPasswordHash() {
        String passwordHash = passwordEncoder.encode("s3cret-password");
        OperatorUser user = OperatorUser.create("operator", passwordHash, "Operations User", OperatorRole.OPERATIONS);
        user.prePersist();
        when(operatorUserRepository.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(new LoginRequest("operator", "s3cret-password"));

        assertThat(response.operatorId()).isNotNull();
        assertThat(response.username()).isEqualTo("operator");
        assertThat(response.displayName()).isEqualTo("Operations User");
        assertThat(response.role()).isEqualTo(OperatorRole.OPERATIONS);
        assertThat(response.authenticatedAt()).isNotNull();
        verify(operatorUserRepository).findByUsernameIgnoreCase("operator");
    }

    @Test
    void loginRejectsPasswordThatDoesNotMatchBcryptHash() {
        String passwordHash = passwordEncoder.encode("correct-password");
        OperatorUser user = OperatorUser.create("operator", passwordHash, "Operations User", OperatorRole.OPERATIONS);
        when(operatorUserRepository.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("operator", "wrong-password")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or password.");
    }
}