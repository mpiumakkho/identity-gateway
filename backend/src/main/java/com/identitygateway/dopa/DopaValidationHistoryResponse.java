package com.identitygateway.dopa;

import java.time.Instant;
import java.util.UUID;

public record DopaValidationHistoryResponse(
        UUID attemptId,
        String validationStatus,
        String identitySource,
        String responseCode,
        String responseMessage,
        String consentReference,
        Instant validatedAt
) {

    public static DopaValidationHistoryResponse from(DopaValidationAttempt attempt) {
        return new DopaValidationHistoryResponse(
                attempt.getId(),
                attempt.getResultStatus().name(),
                attempt.getIdentitySource().name(),
                attempt.getResponseCode(),
                attempt.getResponseMessage(),
                attempt.getConsentReference(),
                attempt.getValidatedAt()
        );
    }
}
