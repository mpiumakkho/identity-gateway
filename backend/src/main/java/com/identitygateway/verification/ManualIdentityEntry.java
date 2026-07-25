package com.identitygateway.verification;

import com.identitygateway.identity.NormalizedManualIdentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "manual_identity_entries")
public class ManualIdentityEntry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private VerificationSessionEntity session;

    @Column(name = "national_id", nullable = false, length = 13)
    private String nationalId;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "laser_code", nullable = false, length = 20)
    private String laserCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ManualIdentityEntry() {
    }

    private ManualIdentityEntry(VerificationSessionEntity session) {
        this.session = session;
    }

    public static ManualIdentityEntry create(VerificationSessionEntity session, NormalizedManualIdentity identity) {
        return new ManualIdentityEntry(session).update(identity);
    }

    public ManualIdentityEntry update(NormalizedManualIdentity identity) {
        this.nationalId = identity.nationalId();
        this.title = identity.title();
        this.firstName = identity.firstName();
        this.lastName = identity.lastName();
        this.dateOfBirth = identity.dateOfBirth();
        this.laserCode = identity.laserCode();
        return this;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public VerificationSessionEntity getSession() {
        return session;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getLaserCode() {
        return laserCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
