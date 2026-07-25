package com.identitygateway.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record AuthenticatedOperator(
        UUID operatorId,
        String username,
        String displayName,
        OperatorRole role,
        Instant sessionExpiresAt
) {

    public static AuthenticatedOperator from(OperatorUser user, Instant sessionExpiresAt) {
        return new AuthenticatedOperator(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                sessionExpiresAt
        );
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}