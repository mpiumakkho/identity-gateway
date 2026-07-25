package com.identitygateway.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VerificationSessionRepository extends JpaRepository<VerificationSessionEntity, UUID> {
}