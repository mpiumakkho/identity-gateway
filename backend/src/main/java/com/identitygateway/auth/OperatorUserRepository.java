package com.identitygateway.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperatorUserRepository extends JpaRepository<OperatorUser, UUID> {

    List<OperatorUser> findAllByOrderByCreatedAtDesc();

    Optional<OperatorUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByEnabled(boolean enabled);

    @Query("select count(operator) from OperatorUser operator where operator.lockedUntil > :now")
    long countLockedOperators(@Param("now") Instant now);
}