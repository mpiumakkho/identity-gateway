package com.identitygateway.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityDataProtectorTest {

    @Test
    void maskNationalIdKeepsOnlyRequiredVisibleDigits() {
        assertThat(IdentityDataProtector.maskNationalId("1234567890123")).isEqualTo("123******0123");
    }

    @Test
    void maskNationalIdFullyMasksInvalidValues() {
        assertThat(IdentityDataProtector.maskNationalId(null)).isEqualTo("*************");
        assertThat(IdentityDataProtector.maskNationalId("123")).isEqualTo("*************");
    }
}