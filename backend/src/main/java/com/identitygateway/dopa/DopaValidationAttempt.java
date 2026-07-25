package com.identitygateway.dopa;

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
@Table(name = "dopa_validation_attempts")
public class DopaValidationAttempt {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VerificationSessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_source", nullable = false, length = 40)
    private DopaIdentitySource identitySource;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 40)
    private DopaValidationResultStatus resultStatus;

    @Column(name = "response_code", nullable = false, length = 40)
    private String responseCode;

    @Column(name = "response_message", nullable = false, length = 255)
    private String responseMessage;

    @Column(name = "consent_reference", nullable = false, length = 80)
    private String consentReference;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DopaValidationAttempt() {
    }

    private DopaValidationAttempt(VerificationSessionEntity session) {
        this.session = session;
    }

    public static DopaValidationAttempt create(
            VerificationSessionEntity session,
            DopaIdentitySource identitySource,
            DopaGatewayResult result,
            String consentReference
    ) {
        DopaValidationAttempt attempt = new DopaValidationAttempt(session);
        attempt.identitySource = identitySource;
        attempt.resultStatus = result.status();
        attempt.responseCode = result.responseCode();
        attempt.responseMessage = result.responseMessage();
        attempt.consentReference = consentReference.trim();
        return attempt;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (validatedAt == null) {
            validatedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public VerificationSessionEntity getSession() {
        return session;
    }

    public DopaIdentitySource getIdentitySource() {
        return identitySource;
    }

    public DopaValidationResultStatus getResultStatus() {
        return resultStatus;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getConsentReference() {
        return consentReference;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}