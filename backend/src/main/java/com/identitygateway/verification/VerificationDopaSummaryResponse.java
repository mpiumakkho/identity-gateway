package com.identitygateway.verification;

import java.time.Instant;

public record VerificationDopaSummaryResponse(
        String validationStatus,
        String identitySource,
        String responseCode,
        String responseMessage,
        String consentReference,
        Instant validatedAt
) {
}