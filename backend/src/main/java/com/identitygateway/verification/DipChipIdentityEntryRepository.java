package com.identitygateway.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DipChipIdentityEntryRepository extends JpaRepository<DipChipIdentityEntry, UUID> {

    Optional<DipChipIdentityEntry> findBySessionId(UUID sessionId);
}