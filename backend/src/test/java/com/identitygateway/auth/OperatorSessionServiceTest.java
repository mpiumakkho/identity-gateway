package com.identitygateway.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorSessionServiceTest {

    @Mock
    private OperatorSessionRepository operatorSessionRepository;

    @Mock
    private SessionTokenGenerator tokenGenerator;

    private final TokenHashingService tokenHashingService = new TokenHashingService();
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-25T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void createSessionStoresTokenHashAndReturnsRawTokenOnce() {
        OperatorSessionService service = service(Duration.ofHours(8));
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        String rawToken = "raw-token-value";
        String expectedHash = tokenHashingService.hash(rawToken);
        when(tokenGenerator.generateToken()).thenReturn(rawToken);

        IssuedOperatorSession issuedSession = service.createSession(operator);

        ArgumentCaptor<OperatorSession> sessionCaptor = ArgumentCaptor.forClass(OperatorSession.class);
        verify(operatorSessionRepository).save(sessionCaptor.capture());
        OperatorSession savedSession = sessionCaptor.getValue();

        assertThat(issuedSession.accessToken()).isEqualTo(rawToken);
        assertThat(issuedSession.expiresAt()).isEqualTo(Instant.parse("2026-07-25T08:00:00Z"));
        assertThat(savedSession.getOperator()).isSameAs(operator);
        assertThat(savedSession.getTokenHash()).isEqualTo(expectedHash);
        assertThat(savedSession.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(savedSession.getExpiresAt()).isEqualTo(Instant.parse("2026-07-25T08:00:00Z"));
    }

    @Test
    void authenticateReturnsOperatorForActiveSession() {
        OperatorSessionService service = service(Duration.ofHours(8));
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        operator.prePersist();
        OperatorSession session = OperatorSession.create(operator, tokenHashingService.hash("valid-token"), Instant.parse("2026-07-25T01:00:00Z"));
        when(operatorSessionRepository.findByTokenHash(tokenHashingService.hash("valid-token"))).thenReturn(Optional.of(session));

        Optional<AuthenticatedOperator> authenticated = service.authenticate("valid-token");

        assertThat(authenticated).isPresent();
        assertThat(authenticated.orElseThrow().operatorId()).isEqualTo(operator.getId());
        assertThat(authenticated.orElseThrow().username()).isEqualTo("operator");
    }

    @Test
    void revokeMarksActiveSessionRevoked() {
        OperatorSessionService service = service(Duration.ofHours(8));
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        OperatorSession session = OperatorSession.create(operator, tokenHashingService.hash("valid-token"), Instant.parse("2026-07-25T01:00:00Z"));
        when(operatorSessionRepository.findByTokenHash(tokenHashingService.hash("valid-token"))).thenReturn(Optional.of(session));

        service.revoke("valid-token");

        assertThat(session.getRevokedAt()).isEqualTo(Instant.parse("2026-07-25T00:00:00Z"));
    }

    @Test
    void revokeActiveSessionsExceptKeepsCurrentSessionActive() {
        OperatorSessionService service = service(Duration.ofHours(8));
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        operator.prePersist();
        OperatorSession currentSession = OperatorSession.create(operator, tokenHashingService.hash("current-token"), Instant.parse("2026-07-25T01:00:00Z"));
        OperatorSession otherSession = OperatorSession.create(operator, tokenHashingService.hash("other-token"), Instant.parse("2026-07-25T01:00:00Z"));
        when(operatorSessionRepository.findByOperatorIdAndRevokedAtIsNull(operator.getId())).thenReturn(List.of(currentSession, otherSession));

        service.revokeActiveSessionsExcept(operator, "current-token");

        assertThat(currentSession.getRevokedAt()).isNull();
        assertThat(otherSession.getRevokedAt()).isEqualTo(Instant.parse("2026-07-25T00:00:00Z"));
    }

    private OperatorSessionService service(Duration sessionTtl) {
        return new OperatorSessionService(
                operatorSessionRepository,
                tokenGenerator,
                tokenHashingService,
                clock,
                sessionTtl
        );
    }
}
