package com.identitygateway.identity;

public final class IdentityDataProtector {

    private static final int NATIONAL_ID_LENGTH = 13;
    private static final String FULL_MASK = "*************";

    private IdentityDataProtector() {
    }

    public static String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() != NATIONAL_ID_LENGTH) {
            return FULL_MASK;
        }

        return nationalId.substring(0, 3) + "******" + nationalId.substring(9);
    }
}