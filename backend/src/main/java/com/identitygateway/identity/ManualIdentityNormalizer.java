package com.identitygateway.identity;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ManualIdentityNormalizer {

    public NormalizedManualIdentity normalize(
            String nationalId,
            String title,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String laserCode
    ) {
        return new NormalizedManualIdentity(
                IdentityTextNormalizer.clean(nationalId),
                IdentityTextNormalizer.clean(title),
                IdentityTextNormalizer.clean(firstName),
                IdentityTextNormalizer.clean(lastName),
                dateOfBirth,
                IdentityTextNormalizer.upperClean(laserCode)
        );
    }
}