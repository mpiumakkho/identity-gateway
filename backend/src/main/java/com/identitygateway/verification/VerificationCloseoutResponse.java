package com.identitygateway.verification;

import java.time.Instant;
import java.util.UUID;

public record VerificationCloseoutResponse(
        UUID transactionId,
        String sessionStatus,
        String decision,
        String notes,
        SessionOperatorResponse decidedBy,
        Instant decidedAt
) {
}