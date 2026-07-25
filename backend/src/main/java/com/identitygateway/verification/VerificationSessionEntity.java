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
@Table(name = "verification_sessions")
public class VerificationSessionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private OperatorUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationSessionEntity() {
    }

    private VerificationSessionEntity(VerificationMethod method, VerificationStatus status, OperatorUser createdBy) {
        this.method = method;
        this.status = status;
        this.createdBy = createdBy;
    }

    public static VerificationSessionEntity create(VerificationMethod method, OperatorUser createdBy) {
        return new VerificationSessionEntity(method, VerificationStatus.CREATED, createdBy);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public VerificationMethod getMethod() {
        return method;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void markIdentityCaptured() {
        status = VerificationStatus.IDENTITY_CAPTURED;
    }

    public void markDopaVerified() {
        status = VerificationStatus.DOPA_VERIFIED;
    }

    public void markDopaRejected() {
        status = VerificationStatus.DOPA_REJECTED;
    }

    public void close(VerificationDecision decision) {
        status = decision == VerificationDecision.APPROVED ? VerificationStatus.APPROVED : VerificationStatus.REJECTED;
    }

    public boolean isClosed() {
        return status == VerificationStatus.APPROVED || status == VerificationStatus.REJECTED;
    }

    public OperatorUser getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}