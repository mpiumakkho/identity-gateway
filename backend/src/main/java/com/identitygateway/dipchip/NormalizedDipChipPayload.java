package com.identitygateway.dipchip;

import java.time.LocalDate;

public record NormalizedDipChipPayload(
        String nationalId,
        String title,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String laserCode,
        LocalDate cardIssueDate,
        LocalDate cardExpiryDate,
        String readerName,
        String readerSerialNumber,
        String rawPayload
) {
}