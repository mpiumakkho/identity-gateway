package com.identitygateway.verification;

import com.identitygateway.auth.OperatorUser;
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
@Table(name = "verification_decisions")
public class VerificationDecisionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VerificationSessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationDecision decision;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decided_by", nullable = false)
    private OperatorUser decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationDecisionEntity() {
    }

    private VerificationDecisionEntity(
            VerificationSessionEntity session,
            VerificationDecision decision,
            String notes,
            OperatorUser decidedBy
    ) {
        this.session = session;
        this.decision = decision;
        this.notes = normalizeNotes(notes);
        this.decidedBy = decidedBy;
    }

    public static VerificationDecisionEntity create(
            VerificationSessionEntity session,
            VerificationDecision decision,
            String notes,
            OperatorUser decidedBy
    ) {
        return new VerificationDecisionEntity(session, decision, notes, decidedBy);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (decidedAt == null) {
            decidedAt = Instant.now();
        }
        if (createdAt == null) {
            createdAt = decidedAt;
        }
    }

    public VerificationSessionEntity getSession() {
        return session;
    }

    public VerificationDecision getDecision() {
        return decision;
    }

    public String getNotes() {
        return notes;
    }

    public OperatorUser getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    private static String normalizeNotes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}