package com.identitygateway.auth;

import com.identitygateway.common.error.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OperatorSessionService {

    private final OperatorSessionRepository operatorSessionRepository;
    private final SessionTokenGenerator tokenGenerator;
    private final TokenHashingService tokenHashingService;
    private final Clock clock;
    private final Duration sessionTtl;

    public OperatorSessionService(
            OperatorSessionRepository operatorSessionRepository,
            SessionTokenGenerator tokenGenerator,
            TokenHashingService tokenHashingService,
            Clock clock,
            @Value("${app.auth.session-ttl:PT8H}") Duration sessionTtl
    ) {
        this.operatorSessionRepository = operatorSessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashingService = tokenHashingService;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    @Transactional
    public IssuedOperatorSession createSession(OperatorUser operator) {
        String accessToken = tokenGenerator.generateToken();
        Instant expiresAt = clock.instant().plus(sessionTtl);
        String tokenHash = tokenHashingService.hash(accessToken);

        operatorSessionRepository.save(OperatorSession.create(operator, tokenHash, expiresAt));

        return new IssuedOperatorSession(accessToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedOperator> authenticate(String accessToken) {
        Instant now = clock.instant();
        return operatorSessionRepository.findByTokenHash(tokenHashingService.hash(accessToken))
                .filter(session -> session.isActive(now))
                .map(this::toAuthenticatedOperator)
                .filter(operator -> operator != null);
    }

    @Transactional(readOnly = true)
    public List<OperatorSessionResponse> activeSessions(OperatorUser operator, String activeAccessToken) {
        Instant now = clock.instant();
        String activeTokenHash = tokenHashingService.hash(activeAccessToken);
        return operatorSessionRepository.findByOperatorIdAndRevokedAtIsNull(operator.getId()).stream()
                .filter(session -> session.isActive(now))
                .map(session -> OperatorSessionResponse.from(session, activeTokenHash))
                .toList();
    }

    @Transactional
    public SessionRevocationResponse revokeSession(OperatorUser operator, UUID sessionId, String activeAccessToken) {
        String activeTokenHash = tokenHashingService.hash(activeAccessToken);
        OperatorSession session = operatorSessionRepository.findById(sessionId)
                .filter(candidate -> candidate.getOperator().getId().equals(operator.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Operator session not found."));

        if (session.getTokenHash().equals(activeTokenHash)) {
            throw new IllegalArgumentException("Current session cannot be revoked from active sessions.");
        }

        Instant now = clock.instant();
        if (session.isActive(now)) {
            session.revoke(now);
        }

        return new SessionRevocationResponse(true);
    }

    @Transactional
    public void revokeActiveSessions(OperatorUser operator) {
        Instant now = clock.instant();
        operatorSessionRepository.findByOperatorIdAndRevokedAtIsNull(operator.getId()).stream()
                .filter(session -> session.isActive(now))
                .forEach(session -> session.revoke(now));
    }

    @Transactional
    public void revokeActiveSessionsExcept(OperatorUser operator, String activeAccessToken) {
        Instant now = clock.instant();
        String activeTokenHash = tokenHashingService.hash(activeAccessToken);
        operatorSessionRepository.findByOperatorIdAndRevokedAtIsNull(operator.getId()).stream()
                .filter(session -> session.isActive(now))
                .filter(session -> !session.getTokenHash().equals(activeTokenHash))
                .forEach(session -> session.revoke(now));
    }

    @Transactional
    public void revoke(String accessToken) {
        Instant now = clock.instant();
        operatorSessionRepository.findByTokenHash(tokenHashingService.hash(accessToken))
                .filter(session -> session.isActive(now))
                .ifPresent(session -> session.revoke(now));
    }

    private AuthenticatedOperator toAuthenticatedOperator(OperatorSession session) {
        OperatorUser operator = session.getOperator();

        if (!operator.isEnabled()) {
            return null;
        }

        return AuthenticatedOperator.from(operator, session.getExpiresAt());
    }
}
