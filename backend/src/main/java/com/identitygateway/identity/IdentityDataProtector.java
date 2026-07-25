package com.identitygateway.identity;

public final class IdentityDataProtector {

    private static final int NATIONAL_ID_LENGTH = 13;
    private static final String FULL_MASK = "*************";

    private IdentityDataProtector() {
    }

    public static boolean isValidNationalId(String nationalId) {
        if (nationalId == null || !nationalId.matches("\\d{" + NATIONAL_ID_LENGTH + "}")) {
            return false;
        }

        int sum = 0;
        for (int index = 0; index < NATIONAL_ID_LENGTH - 1; index++) {
            int digit = Character.digit(nationalId.charAt(index), 10);
            sum += digit * (NATIONAL_ID_LENGTH - index);
        }

        int expectedCheckDigit = (11 - (sum % 11)) % 10;
        int actualCheckDigit = Character.digit(nationalId.charAt(NATIONAL_ID_LENGTH - 1), 10);
        return expectedCheckDigit == actualCheckDigit;
    }

    public static String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() != NATIONAL_ID_LENGTH) {
            return FULL_MASK;
        }

        return nationalId.substring(0, 3) + "******" + nationalId.substring(9);
    }
}
