package com.identitygateway.auth;

import java.time.Instant;
import java.util.UUID;

public record OperatorSessionResponse(
        UUID sessionId,
        boolean current,
        Instant createdAt,
        Instant expiresAt
) {

    public static OperatorSessionResponse from(OperatorSession session, String currentTokenHash) {
        return new OperatorSessionResponse(
                session.getId(),
                session.getTokenHash().equals(currentTokenHash),
                session.getCreatedAt(),
                session.getExpiresAt()
        );
    }
}
