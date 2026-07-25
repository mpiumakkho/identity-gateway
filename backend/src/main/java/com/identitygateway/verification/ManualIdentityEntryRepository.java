package com.identitygateway.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ManualIdentityEntryRepository extends JpaRepository<ManualIdentityEntry, UUID> {

    Optional<ManualIdentityEntry> findBySessionId(UUID sessionId);
}
