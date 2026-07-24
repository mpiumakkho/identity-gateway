package com.identitygateway.verification;

import java.time.Instant;
import java.util.UUID;

public record VerificationSessionResponse(
        UUID transactionId,
        String method,
        String status,
        Instant createdAt
) {
}