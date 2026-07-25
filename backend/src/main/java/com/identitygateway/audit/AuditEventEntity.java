package com.identitygateway.audit;

import com.identitygateway.auth.OperatorUser;
import com.identitygateway.verification.VerificationSessionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private AuditEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private OperatorUser operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private VerificationSessionEntity session;

    @Column(nullable = false, length = 255)
    private String summary;

    @Column(name = "metadata_json", length = 2000)
    private String metadataJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEventEntity() {
    }

    private AuditEventEntity(
            AuditEventType eventType,
            OperatorUser operator,
            VerificationSessionEntity session,
            String summary,
            String metadataJson
    ) {
        this.eventType = eventType;
        this.operator = operator;
        this.session = session;
        this.summary = summary;
        this.metadataJson = metadataJson;
    }

    static AuditEventEntity create(
            AuditEventType eventType,
            OperatorUser operator,
            VerificationSessionEntity session,
            String summary,
            String metadataJson
    ) {
        return new AuditEventEntity(eventType, operator, session, summary, metadataJson);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public OperatorUser getOperator() {
        return operator;
    }

    public VerificationSessionEntity getSession() {
        return session;
    }

    public String getSummary() {
        return summary;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}