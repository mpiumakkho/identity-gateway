package com.identitygateway.audit;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    @EntityGraph(attributePaths = {"operator", "session"})
    List<AuditEventEntity> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);
}