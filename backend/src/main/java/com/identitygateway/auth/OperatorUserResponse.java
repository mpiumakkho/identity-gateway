package com.identitygateway.auth;

import java.time.Instant;
import java.util.UUID;

public record OperatorUserResponse(
        UUID operatorId,
        String username,
        String displayName,
        OperatorRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Instant disabledAt
) {

    public static OperatorUserResponse from(OperatorUser user) {
        return new OperatorUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDisabledAt()
        );
    }
}