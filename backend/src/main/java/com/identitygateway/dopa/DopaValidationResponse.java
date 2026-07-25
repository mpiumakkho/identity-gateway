package com.identitygateway.dopa;

import java.time.Instant;
import java.util.UUID;

public record DopaValidationResponse(
        UUID transactionId,
        String sessionStatus,
        String validationStatus,
        String identitySource,
        String maskedNationalId,
        String responseCode,
        String responseMessage,
        String consentReference,
        Instant validatedAt
) {
}