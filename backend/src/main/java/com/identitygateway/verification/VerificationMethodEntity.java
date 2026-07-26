package com.identitygateway.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "verification_methods")
public class VerificationMethodEntity {

    @Id
    @Column(nullable = false, length = 40)
    private String id;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VerificationMethodEntity() {
    }

    private VerificationMethodEntity(String id, String label, String description, boolean enabled, int sortOrder) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    public static VerificationMethodEntity create(VerificationMethod method, boolean enabled, int sortOrder) {
        return new VerificationMethodEntity(method.name(), method.label(), method.description(), enabled, sortOrder);
    }

    @PrePersist
    void prePersist() {
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

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
