package com.identitygateway.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operator_users")
public class OperatorUser {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OperatorRole role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected OperatorUser() {
    }

    private OperatorUser(String username, String passwordHash, String displayName, OperatorRole role, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.enabled = enabled;
    }

    public static OperatorUser create(String username, String passwordHash, String displayName, OperatorRole role) {
        return new OperatorUser(username, passwordHash, displayName, role, true);
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
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        clearLoginLock();
    }

    public String getDisplayName() {
        return displayName;
    }

    public OperatorRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable(Instant disabledAt) {
        this.enabled = false;
        this.disabledAt = disabledAt;
    }

    public void enable() {
        this.enabled = true;
        this.disabledAt = null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(Instant lockedUntil) {
        failedLoginAttempts++;
        this.lockedUntil = lockedUntil;
    }

    public void clearLoginLock() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}