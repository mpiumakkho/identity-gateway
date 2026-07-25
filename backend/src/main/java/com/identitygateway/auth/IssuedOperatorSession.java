package com.identitygateway.auth;

import java.time.Instant;

public record IssuedOperatorSession(
        String accessToken,
        Instant expiresAt
) {
}