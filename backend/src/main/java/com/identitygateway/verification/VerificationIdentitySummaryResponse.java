package com.identitygateway.verification;

import java.time.Instant;
import java.time.LocalDate;

public record VerificationIdentitySummaryResponse(
        String source,
        String maskedNationalId,
        String title,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        LocalDate cardIssueDate,
        LocalDate cardExpiryDate,
        String readerName,
        String readerSerialNumber,
        Instant updatedAt
) {
}