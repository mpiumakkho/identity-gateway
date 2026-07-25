package com.identitygateway.verification;

import java.time.Instant;

public record VerificationDecisionSummaryResponse(
        String decision,
        String notes,
        SessionOperatorResponse decidedBy,
        Instant decidedAt
) {
}