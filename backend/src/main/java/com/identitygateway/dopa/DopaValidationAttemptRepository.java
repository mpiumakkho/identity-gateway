package com.identitygateway.dopa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DopaValidationAttemptRepository extends JpaRepository<DopaValidationAttempt, UUID> {

    List<DopaValidationAttempt> findTop10BySessionIdOrderByValidatedAtDesc(UUID sessionId);
}