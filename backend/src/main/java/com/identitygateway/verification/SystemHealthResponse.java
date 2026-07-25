package com.identitygateway.verification;

import java.time.Instant;

public record SystemHealthResponse(
        String service,
        String status,
        String databaseStatus,
        Instant checkedAt
) {
}
