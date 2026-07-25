package com.identitygateway.identity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ManualIdentityNormalizerTest {

    private final ManualIdentityNormalizer normalizer = new ManualIdentityNormalizer();

    @Test
    void normalizeTrimsCompressesWhitespaceAndUppercasesLaserCode() {
        NormalizedManualIdentity identity = normalizer.normalize(
                " 1234567890121 ",
                " Mr. ",
                " Somchai  Middle ",
                " Jaidee ",
                LocalDate.parse("1990-01-31"),
                " jt1234567890 "
        );

        assertThat(identity.nationalId()).isEqualTo("1234567890121");
        assertThat(identity.title()).isEqualTo("Mr.");
        assertThat(identity.firstName()).isEqualTo("Somchai Middle");
        assertThat(identity.lastName()).isEqualTo("Jaidee");
        assertThat(identity.laserCode()).isEqualTo("JT1234567890");
    }
}