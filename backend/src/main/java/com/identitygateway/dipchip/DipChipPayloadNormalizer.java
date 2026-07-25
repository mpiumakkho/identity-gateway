package com.identitygateway.dipchip;

import com.identitygateway.identity.IdentityTextNormalizer;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
                IdentityTextNormalizer.clean(nationalId),
                IdentityTextNormalizer.clean(title),
                IdentityTextNormalizer.clean(firstName),
                IdentityTextNormalizer.clean(lastName),
                dateOfBirth,
                IdentityTextNormalizer.upperClean(laserCode),
                cardIssueDate,
                cardExpiryDate,
                IdentityTextNormalizer.clean(readerName),
                IdentityTextNormalizer.upperClean(readerSerialNumber),
                rawPayload.trim()
        );

        if (payload.cardExpiryDate().isBefore(payload.cardIssueDate())) {
            throw new IllegalArgumentException("Card expiry date must be on or after the issue date.");
        }

        return payload;
    }
}