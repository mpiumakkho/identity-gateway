package com.identitygateway.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    @EntityGraph(attributePaths = {"operator", "session"})
    List<AuditEventEntity> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);

    @EntityGraph(attributePaths = {"operator", "session"})
    @Query("""
            select event from AuditEventEntity event
            where (:eventType is null or event.eventType = :eventType)
              and (:operatorId is null or event.operator.id = :operatorId)
            order by event.occurredAt desc
            """)
    List<AuditEventEntity> searchRecent(
            @Param("eventType") AuditEventType eventType,
            @Param("operatorId") UUID operatorId,
            Pageable pageable
    );
}
