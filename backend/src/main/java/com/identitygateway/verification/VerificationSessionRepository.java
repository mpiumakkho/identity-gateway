package com.identitygateway.verification;

import org.springframework.data.domain.Pageable;
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
    @Query("""
            select session from VerificationSessionEntity session
            where (:method is null or session.method = :method)
              and (:status is null or session.status = :status)
            order by session.createdAt desc
            """)
    List<VerificationSessionEntity> findRecent(
            @Param("method") VerificationMethod method,
            @Param("status") VerificationStatus status,
            Pageable pageable
    );

    @Query("""
            select session.status as status, count(session) as total
            from VerificationSessionEntity session
            group by session.status
            """)
    List<VerificationStatusMetric> countByStatus();

    long countByStatus(VerificationStatus status);

    @Query("""
            select session.method as method, count(session) as total
            from VerificationSessionEntity session
            group by session.method
            """)
    List<VerificationMethodMetric> countByMethod();

    long countByMethod(VerificationMethod method);

    @EntityGraph(attributePaths = "createdBy")
    @Query("select session from VerificationSessionEntity session where session.id = :id")
    Optional<VerificationSessionEntity> findDetailById(@Param("id") UUID id);
}
