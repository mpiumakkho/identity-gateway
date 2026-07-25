package com.identitygateway.verification;

import java.time.Instant;
import java.util.UUID;

public record VerificationSessionDetailResponse(
        UUID transactionId,
        String method,
        String status,
        SessionOperatorResponse createdBy,
        Instant createdAt,
        VerificationIdentitySummaryResponse identity,
        VerificationDopaSummaryResponse dopaValidation,
        VerificationDecisionSummaryResponse closeout
) {
}