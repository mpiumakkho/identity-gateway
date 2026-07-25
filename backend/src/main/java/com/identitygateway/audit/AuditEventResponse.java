package com.identitygateway.audit;

import com.identitygateway.verification.SessionOperatorResponse;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID eventId,
        String eventType,
        UUID transactionId,
        SessionOperatorResponse operator,
        String summary,
        String metadataJson,
        Instant occurredAt
) {
}