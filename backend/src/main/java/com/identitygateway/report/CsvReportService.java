package com.identitygateway.report;

import com.identitygateway.audit.AuditEventResponse;
import com.identitygateway.verification.SessionOperatorResponse;
import com.identitygateway.verification.VerificationSessionResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CsvReportService {

    public String verificationSessions(List<VerificationSessionResponse> sessions) {
        CsvBuilder csv = new CsvBuilder()
                .row("transactionId", "method", "status", "createdByOperatorId", "createdByUsername", "createdByDisplayName", "createdAt");
        for (VerificationSessionResponse session : sessions) {
            SessionOperatorResponse operator = session.createdBy();
            csv.row(
                    value(session.transactionId()),
                    session.method(),
                    session.status(),
                    operator == null ? "" : value(operator.operatorId()),
                    operator == null ? "" : operator.username(),
                    operator == null ? "" : operator.displayName(),
                    value(session.createdAt())
            );
        }
        return csv.toString();
    }

    public String auditEvents(List<AuditEventResponse> events) {
        CsvBuilder csv = new CsvBuilder()
                .row("eventId", "eventType", "transactionId", "operatorId", "username", "displayName", "summary", "metadataJson", "occurredAt");
        for (AuditEventResponse event : events) {
            SessionOperatorResponse operator = event.operator();
            csv.row(
                    value(event.eventId()),
                    event.eventType(),
                    value(event.transactionId()),
                    operator == null ? "" : value(operator.operatorId()),
                    operator == null ? "" : operator.username(),
                    operator == null ? "" : operator.displayName(),
                    event.summary(),
                    event.metadataJson(),
                    value(event.occurredAt())
            );
        }
        return csv.toString();
    }

    private static String value(UUID value) {
        return value == null ? "" : value.toString();
    }

    private static String value(Instant value) {
        return value == null ? "" : value.toString();
    }

    private static class CsvBuilder {
        private final StringBuilder content = new StringBuilder();

        CsvBuilder row(String... values) {
            for (int index = 0; index < values.length; index++) {
                if (index > 0) {
                    content.append(',');
                }
                content.append(escape(values[index]));
            }
            content.append('\n');
            return this;
        }

        @Override
        public String toString() {
            return content.toString();
        }

        private static String escape(String value) {
            String safeValue = value == null ? "" : protectFormula(value);
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }

        private static String protectFormula(String value) {
            if (value.isEmpty()) {
                return value;
            }
            char first = value.charAt(0);
            return first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r' || first == '\n'
                    ? "'" + value
                    : value;
        }
    }
}