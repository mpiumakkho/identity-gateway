package com.identitygateway.dopa;

import java.time.LocalDate;

public record DopaIdentitySnapshot(
        DopaIdentitySource source,
        String nationalId,
        String title,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String laserCode
) {
}