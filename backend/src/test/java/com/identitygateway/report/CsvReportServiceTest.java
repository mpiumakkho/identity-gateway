package com.identitygateway.report;

import com.identitygateway.audit.AuditEventResponse;
import com.identitygateway.verification.SessionOperatorResponse;
import com.identitygateway.verification.VerificationSessionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReportServiceTest {

    private final CsvReportService csvReportService = new CsvReportService();

    @Test
    void verificationSessionsExportsCsvWithEscapedColumns() {
        String csv = csvReportService.verificationSessions(List.of(new VerificationSessionResponse(
                UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a"),
                "DIP_CHIP",
                "CREATED",
                new SessionOperatorResponse(
                        UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                        "operator",
                        "Ops, \"User\""
                ),
                Instant.parse("2026-07-25T00:00:00Z")
        )));

        assertThat(csv).isEqualTo("\"transactionId\",\"method\",\"status\",\"createdByOperatorId\",\"createdByUsername\",\"createdByDisplayName\",\"createdAt\"\n"
                + "\"b9d38258-8ec4-4645-a6ca-e901e1c1766a\",\"DIP_CHIP\",\"CREATED\",\"9e04e2eb-d74a-4d55-987c-f38660aa3060\",\"operator\",\"Ops, \"\"User\"\"\",\"2026-07-25T00:00:00Z\"\n");
    }

    @Test
    void auditEventsProtectsFormulaLikeValues() {
        String csv = csvReportService.auditEvents(List.of(new AuditEventResponse(
                UUID.fromString("ca21e048-17f8-4b2f-a6b0-47c5d34172e2"),
                "AUTH_LOGIN_FAILED",
                null,
                null,
                "=cmd|' /C calc'!A0",
                "{\"reason\":\"+formula\"}",
                Instant.parse("2026-07-25T00:00:00Z")
        )));

        assertThat(csv).contains("\"'=cmd|' /C calc'!A0\"");
        assertThat(csv).contains("\"{\"\"reason\"\":\"\"+formula\"\"}\"");
    }
}