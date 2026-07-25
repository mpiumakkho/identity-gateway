package com.identitygateway.auth;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        UUID operatorId,
        String username,
        String displayName,
        OperatorRole role,
        Instant authenticatedAt
) {
}