package com.identitygateway.audit;

import com.identitygateway.auth.OperatorUser;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.verification.SessionOperatorResponse;
import com.identitygateway.verification.VerificationSessionEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final OperatorUserRepository operatorUserRepository;

    public AuditService(AuditEventRepository auditEventRepository, OperatorUserRepository operatorUserRepository) {
        this.auditEventRepository = auditEventRepository;
        this.operatorUserRepository = operatorUserRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordTransactionEvent(
            AuditEventType eventType,
            UUID operatorId,
            VerificationSessionEntity session,
            String summary,
            Map<String, String> metadata
    ) {
        OperatorUser operator = operatorUserRepository.getReferenceById(operatorId);
        auditEventRepository.save(AuditEventEntity.create(eventType, operator, session, summary, toMetadataJson(metadata)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordOperatorEvent(AuditEventType eventType, UUID operatorId, String summary, Map<String, String> metadata) {
        OperatorUser operator = operatorUserRepository.getReferenceById(operatorId);
        auditEventRepository.save(AuditEventEntity.create(eventType, operator, null, summary, toMetadataJson(metadata)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordOperatorEvent(AuditEventType eventType, OperatorUser operator, String summary, Map<String, String> metadata) {
        auditEventRepository.save(AuditEventEntity.create(eventType, operator, null, summary, toMetadataJson(metadata)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSystemEvent(AuditEventType eventType, String summary, Map<String, String> metadata) {
        auditEventRepository.save(AuditEventEntity.create(eventType, null, null, summary, toMetadataJson(metadata)));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> transactionEvents(UUID transactionId) {
        return auditEventRepository.findBySessionIdOrderByOccurredAtAsc(transactionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> recentEvents(String eventType, UUID operatorId, int limit) {
        AuditEventType parsedEventType = parseEventType(eventType);
        int cappedLimit = Math.max(1, Math.min(limit, 100));

        return auditEventRepository.searchRecent(parsedEventType, operatorId, PageRequest.of(0, cappedLimit)).stream()
                .map(this::toResponse)
                .toList();
    }

    public static Map<String, String> metadata(String key, String value) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(key, value);
        return metadata;
    }

    public static Map<String, String> metadata(String key1, String value1, String key2, String value2) {
        Map<String, String> metadata = metadata(key1, value1);
        metadata.put(key2, value2);
        return metadata;
    }

    public static Map<String, String> metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        Map<String, String> metadata = metadata(key1, value1, key2, value2);
        metadata.put(key3, value3);
        return metadata;
    }

    private static AuditEventType parseEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }

        try {
            return AuditEventType.valueOf(eventType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported audit event type: " + eventType, ex);
        }
    }

    private AuditEventResponse toResponse(AuditEventEntity event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType().name(),
                event.getSession() == null ? null : event.getSession().getId(),
                SessionOperatorResponse.from(event.getOperator()),
                event.getSummary(),
                event.getMetadataJson(),
                event.getOccurredAt()
        );
    }

    private static String toMetadataJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        return metadata.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> quote(entry.getKey()) + ":" + quote(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
