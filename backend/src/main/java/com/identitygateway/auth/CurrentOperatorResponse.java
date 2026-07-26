package com.identitygateway.auth;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CurrentOperatorResponse(
        UUID operatorId,
        String username,
        String displayName,
        OperatorRole role,
        Set<OperatorPermission> permissions,
        Instant sessionExpiresAt
) {
    public static CurrentOperatorResponse from(AuthenticatedOperator operator) {
        return new CurrentOperatorResponse(
                operator.operatorId(),
                operator.username(),
                operator.displayName(),
                operator.role(),
                operator.permissions(),
                operator.sessionExpiresAt()
        );
    }
}