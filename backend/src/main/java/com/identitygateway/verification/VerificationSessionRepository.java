package com.identitygateway.verification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationSessionRepository extends JpaRepository<VerificationSessionEntity, UUID> {

    @EntityGraph(attributePaths = "createdBy")
    List<VerificationSessionEntity> findTop20ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "createdBy")
    @Query("select session from VerificationSessionEntity session where session.id = :id")
    Optional<VerificationSessionEntity> findDetailById(@Param("id") UUID id);
}