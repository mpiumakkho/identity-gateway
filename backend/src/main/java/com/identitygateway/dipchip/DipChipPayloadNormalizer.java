package com.identitygateway.dipchip;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class DipChipPayloadNormalizer {

    public NormalizedDipChipPayload normalize(
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
        NormalizedDipChipPayload payload = new NormalizedDipChipPayload(
                clean(nationalId),
                clean(title),
                clean(firstName),
                clean(lastName),
                dateOfBirth,
                upper(clean(laserCode)),
                cardIssueDate,
                cardExpiryDate,
                clean(readerName),
                upper(clean(readerSerialNumber)),
                rawPayload.trim()
        );

        if (payload.cardExpiryDate().isBefore(payload.cardIssueDate())) {
            throw new IllegalArgumentException("Card expiry date must be on or after the issue date.");
        }

        return payload;
    }

    private static String clean(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}