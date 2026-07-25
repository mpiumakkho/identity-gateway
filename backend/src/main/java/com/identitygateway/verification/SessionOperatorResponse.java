package com.identitygateway.verification;

import com.identitygateway.auth.OperatorUser;

import java.util.UUID;

public record SessionOperatorResponse(
        UUID operatorId,
        String username,
        String displayName
) {
    public static SessionOperatorResponse from(OperatorUser operator) {
        if (operator == null) {
            return null;
        }

        return new SessionOperatorResponse(
                operator.getId(),
                operator.getUsername(),
                operator.getDisplayName()
        );
    }
}