package com.identitygateway.verification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    @EntityGraph(attributePaths = "createdBy")
    @Query("""
            select distinct session from VerificationSessionEntity session
            left join ManualIdentityEntry manualIdentity on manualIdentity.session = session
            left join DipChipIdentityEntry dipChipIdentity on dipChipIdentity.session = session
            where (:method is null or session.method = :method)
              and (:statusesEmpty = true or session.status in :statuses)
              and (:createdBy is null or session.createdBy.id = :createdBy)
              and (:createdFrom is null or session.createdAt >= :createdFrom)
              and (:createdTo is null or session.createdAt <= :createdTo)
              and (:identityNationalId is null
                   or manualIdentity.nationalId = :identityNationalId
                   or dipChipIdentity.nationalId = :identityNationalId)
            order by session.createdAt desc
            """)
    List<VerificationSessionEntity> searchRecent(
            @Param("method") VerificationMethod method,
            @Param("statuses") List<VerificationStatus> statuses,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("createdBy") UUID createdBy,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("identityNationalId") String identityNationalId,
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
