package com.identitygateway.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

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
}