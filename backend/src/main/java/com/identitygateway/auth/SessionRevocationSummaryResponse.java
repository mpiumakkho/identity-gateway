package com.identitygateway.auth;

public record SessionRevocationSummaryResponse(
        int revokedSessions
) {
}
