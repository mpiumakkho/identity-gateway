package com.identitygateway.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityDataProtectorTest {

    @Test
    void maskNationalIdKeepsOnlyRequiredVisibleDigits() {
        assertThat(IdentityDataProtector.maskNationalId("1234567890121")).isEqualTo("123******0121");
    }

    @Test
    void isValidNationalIdChecksLengthDigitsAndChecksum() {
        assertThat(IdentityDataProtector.isValidNationalId("1234567890121")).isTrue();
        assertThat(IdentityDataProtector.isValidNationalId("1234567890123")).isFalse();
        assertThat(IdentityDataProtector.isValidNationalId("123")).isFalse();
        assertThat(IdentityDataProtector.isValidNationalId("123456789012A")).isFalse();
    }

    @Test
    void maskNationalIdFullyMasksInvalidValues() {
        assertThat(IdentityDataProtector.maskNationalId(null)).isEqualTo("*************");
        assertThat(IdentityDataProtector.maskNationalId("123")).isEqualTo("*************");
    }
}
