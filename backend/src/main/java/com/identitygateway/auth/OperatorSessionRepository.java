package com.identitygateway.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface OperatorSessionRepository extends JpaRepository<OperatorSession, UUID> {

    Optional<OperatorSession> findByTokenHash(String tokenHash);

    List<OperatorSession> findByOperatorIdAndRevokedAtIsNull(UUID operatorId);

    @Modifying
    @Query("delete from OperatorSession session where session.expiresAt < :expiredBefore or (session.revokedAt is not null and session.revokedAt < :revokedBefore)")
    int deleteExpiredOrRevokedBefore(@Param("expiredBefore") Instant expiredBefore, @Param("revokedBefore") Instant revokedBefore);
}