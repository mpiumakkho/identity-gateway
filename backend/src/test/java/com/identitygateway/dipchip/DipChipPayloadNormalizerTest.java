package com.identitygateway.dipchip;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DipChipPayloadNormalizerTest {

    private final DipChipPayloadNormalizer normalizer = new DipChipPayloadNormalizer();

    @Test
    void normalizeTrimsCompressesWhitespaceAndUppercasesCodes() {
        NormalizedDipChipPayload payload = normalizer.normalize(
                " 1234567890121 ",
                " Mr. ",
                " Somchai  Middle ",
                " Jaidee ",
                LocalDate.parse("1990-01-31"),
                " jt1234567890 ",
                LocalDate.parse("2021-02-01"),
                LocalDate.parse("2031-01-31"),
                " ACR39U  Reader ",
                " rd-001 ",
                " CID=1234567890121;READER=ACR39U "
        );

        assertThat(payload.nationalId()).isEqualTo("1234567890121");
        assertThat(payload.firstName()).isEqualTo("Somchai Middle");
        assertThat(payload.laserCode()).isEqualTo("JT1234567890");
        assertThat(payload.readerName()).isEqualTo("ACR39U Reader");
        assertThat(payload.readerSerialNumber()).isEqualTo("RD-001");
        assertThat(payload.rawPayload()).isEqualTo("CID=1234567890121;READER=ACR39U");
    }

    @Test
    void normalizeRejectsCardExpiryBeforeIssueDate() {
        assertThatThrownBy(() -> normalizer.normalize(
                "1234567890121",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890",
                LocalDate.parse("2031-01-31"),
                LocalDate.parse("2021-02-01"),
                "ACR39U",
                "RD-001",
                "CID=1234567890121;READER=ACR39U"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card expiry date must be on or after the issue date.");
    }
}