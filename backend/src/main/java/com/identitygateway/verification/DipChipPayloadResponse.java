package com.identitygateway.verification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DipChipPayloadResponse(
        UUID transactionId,
        String sessionStatus,
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