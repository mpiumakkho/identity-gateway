package com.identitygateway.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperatorSessionRepository extends JpaRepository<OperatorSession, UUID> {

    Optional<OperatorSession> findByTokenHash(String tokenHash);
}