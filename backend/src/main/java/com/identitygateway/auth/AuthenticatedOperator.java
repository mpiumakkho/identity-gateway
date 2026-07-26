package com.identitygateway.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedOperator(
        UUID operatorId,
        String username,
        String displayName,
        OperatorRole role,
        Set<OperatorPermission> permissions,
        Instant sessionExpiresAt
) {

    public static AuthenticatedOperator from(OperatorUser user, Instant sessionExpiresAt) {
        return new AuthenticatedOperator(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getRole().permissions(),
                sessionExpiresAt
        );
    }

    public Collection<? extends GrantedAuthority> authorities() {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.name())));
        return List.copyOf(authorities);
    }
}