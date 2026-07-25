package com.identitygateway.verification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationDecisionRepository extends JpaRepository<VerificationDecisionEntity, UUID> {

    boolean existsBySessionId(UUID sessionId);

    @EntityGraph(attributePaths = "decidedBy")
    Optional<VerificationDecisionEntity> findBySessionId(UUID sessionId);
}