package com.identitygateway.verification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ManualIdentityResponse(
        UUID transactionId,
        String sessionStatus,
        String maskedNationalId,
        String title,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Instant updatedAt
) {
}
