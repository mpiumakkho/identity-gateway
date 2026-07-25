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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorSessionServiceTest {

    @Mock
    private OperatorSessionRepository operatorSessionRepository;

    @Mock
    private SessionTokenGenerator tokenGenerator;

    @Test
    void createSessionStoresTokenHashAndReturnsRawTokenOnce() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-25T00:00:00Z"), ZoneOffset.UTC);
        TokenHashingService tokenHashingService = new TokenHashingService();
        OperatorSessionService service = new OperatorSessionService(
                operatorSessionRepository,
                tokenGenerator,
                tokenHashingService,
                clock,
                Duration.ofHours(8)
        );
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
}