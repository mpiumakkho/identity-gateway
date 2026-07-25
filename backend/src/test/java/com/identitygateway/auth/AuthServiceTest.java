package com.identitygateway.auth;

import com.identitygateway.audit.AuditService;
import com.identitygateway.common.error.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OperatorUserRepository operatorUserRepository;

    @Mock
    private OperatorSessionService operatorSessionService;

    @Mock
    private AuditService auditService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(operatorUserRepository, passwordEncoder, operatorSessionService, auditService);
    }

    @Test
    void loginAcceptsBcryptPasswordHashAndIssuesSession() {
        String passwordHash = passwordEncoder.encode("s3cret-password");
        OperatorUser user = OperatorUser.create("operator", passwordHash, "Operations User", OperatorRole.OPERATIONS);
        user.prePersist();
        Instant expiresAt = Instant.parse("2026-07-25T08:00:00Z");

        when(operatorUserRepository.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(user));
        when(operatorSessionService.createSession(user)).thenReturn(new IssuedOperatorSession("issued-token", expiresAt));

        LoginResponse response = authService.login(new LoginRequest("operator", "s3cret-password"));

        assertThat(response.operatorId()).isNotNull();
        assertThat(response.username()).isEqualTo("operator");
        assertThat(response.displayName()).isEqualTo("Operations User");
        assertThat(response.role()).isEqualTo(OperatorRole.OPERATIONS);
        assertThat(response.authenticatedAt()).isNotNull();
        assertThat(response.accessToken()).isEqualTo("issued-token");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        verify(operatorUserRepository).findByUsernameIgnoreCase("operator");
        verify(operatorSessionService).createSession(user);
    }

    @Test
    void loginRejectsPasswordThatDoesNotMatchBcryptHash() {
        String passwordHash = passwordEncoder.encode("correct-password");
        OperatorUser user = OperatorUser.create("operator", passwordHash, "Operations User", OperatorRole.OPERATIONS);
        when(operatorUserRepository.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("operator", "wrong-password")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or password.");

        verify(operatorSessionService, never()).createSession(user);
    }

    @Test
    void activeSessionsReturnsCurrentOperatorSessions() {
        OperatorUser user = operator("operator");
        AuthenticatedOperator operator = AuthenticatedOperator.from(user, Instant.parse("2026-07-25T08:00:00Z"));
        OperatorSessionResponse session = new OperatorSessionResponse(
                UUID.randomUUID(),
                true,
                Instant.parse("2026-07-25T00:00:00Z"),
                Instant.parse("2026-07-25T08:00:00Z")
        );

        when(operatorUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(operatorSessionService.activeSessions(user, "current-token")).thenReturn(List.of(session));

        List<OperatorSessionResponse> sessions = authService.activeSessions(operator, "current-token");

        assertThat(sessions).containsExactly(session);
    }

    @Test
    void revokeSessionRevokesCurrentOperatorSession() {
        OperatorUser user = operator("operator");
        AuthenticatedOperator operator = AuthenticatedOperator.from(user, Instant.parse("2026-07-25T08:00:00Z"));
        UUID sessionId = UUID.randomUUID();
        when(operatorUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(operatorSessionService.revokeSession(user, sessionId, "current-token")).thenReturn(new SessionRevocationResponse(true));

        SessionRevocationResponse response = authService.revokeSession(operator, sessionId, "current-token");

        assertThat(response.revoked()).isTrue();
        verify(operatorSessionService).revokeSession(user, sessionId, "current-token");
    }

    @Test
    void changeOwnPasswordVerifiesCurrentPasswordAndKeepsCurrentSession() {
        String passwordHash = passwordEncoder.encode("current-password");
        OperatorUser user = OperatorUser.create("operator", passwordHash, "Operations User", OperatorRole.OPERATIONS);
        user.prePersist();
        AuthenticatedOperator operator = AuthenticatedOperator.from(user, Instant.parse("2026-07-25T08:00:00Z"));

        when(operatorUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        PasswordChangeResponse response = authService.changeOwnPassword(
                operator,
                "current-token",
                new ChangeOwnPasswordRequest("current-password", "new-secret-123")
        );

        assertThat(response.passwordChanged()).isTrue();
        assertThat(passwordEncoder.matches("new-secret-123", user.getPasswordHash())).isTrue();
        verify(operatorSessionService).revokeActiveSessionsExcept(user, "current-token");
    }

    @Test
    void changeOwnPasswordRejectsWrongCurrentPassword() {
        String passwordHash = passwordEncoder.encode("current-password");
        OperatorUser user = OperatorUser.create("operator", passwordHash, "Operations User", OperatorRole.OPERATIONS);
        user.prePersist();
        AuthenticatedOperator operator = AuthenticatedOperator.from(user, Instant.parse("2026-07-25T08:00:00Z"));

        when(operatorUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.changeOwnPassword(
                operator,
                "current-token",
                new ChangeOwnPasswordRequest("wrong-password", "new-secret-123")
        ))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or password.");

        verify(operatorSessionService, never()).revokeActiveSessionsExcept(user, "current-token");
    }

    private static OperatorUser operator(String username) {
        OperatorUser user = OperatorUser.create(username, "hash", "Operations User", OperatorRole.OPERATIONS);
        user.prePersist();
        return user;
    }
}
