package com.identitygateway.identity;

import java.time.LocalDate;

public record NormalizedManualIdentity(
        String nationalId,
        String title,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String laserCode
) {
}