package com.identitygateway.verification;

import com.identitygateway.dipchip.NormalizedDipChipPayload;

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
@Table(name = "dip_chip_identity_entries")
public class DipChipIdentityEntry {

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

    @Column(name = "card_issue_date", nullable = false)
    private LocalDate cardIssueDate;

    @Column(name = "card_expiry_date", nullable = false)
    private LocalDate cardExpiryDate;

    @Column(name = "reader_name", nullable = false, length = 80)
    private String readerName;

    @Column(name = "reader_serial_number", nullable = false, length = 80)
    private String readerSerialNumber;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DipChipIdentityEntry() {
    }

    private DipChipIdentityEntry(VerificationSessionEntity session) {
        this.session = session;
    }

    public static DipChipIdentityEntry create(VerificationSessionEntity session, NormalizedDipChipPayload payload) {
        return new DipChipIdentityEntry(session).update(payload);
    }

    public DipChipIdentityEntry update(NormalizedDipChipPayload payload) {
        this.nationalId = payload.nationalId();
        this.title = payload.title();
        this.firstName = payload.firstName();
        this.lastName = payload.lastName();
        this.dateOfBirth = payload.dateOfBirth();
        this.laserCode = payload.laserCode();
        this.cardIssueDate = payload.cardIssueDate();
        this.cardExpiryDate = payload.cardExpiryDate();
        this.readerName = payload.readerName();
        this.readerSerialNumber = payload.readerSerialNumber();
        this.rawPayload = payload.rawPayload();
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

    public LocalDate getCardIssueDate() {
        return cardIssueDate;
    }

    public LocalDate getCardExpiryDate() {
        return cardExpiryDate;
    }

    public String getReaderName() {
        return readerName;
    }

    public String getReaderSerialNumber() {
        return readerSerialNumber;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}