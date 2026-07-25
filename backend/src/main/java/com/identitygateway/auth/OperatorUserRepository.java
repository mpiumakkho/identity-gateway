package com.identitygateway.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperatorUserRepository extends JpaRepository<OperatorUser, UUID> {

    Optional<OperatorUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}