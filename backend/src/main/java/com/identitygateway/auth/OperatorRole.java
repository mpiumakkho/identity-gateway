package com.identitygateway.auth;

import java.util.EnumSet;
import java.util.Set;

public enum OperatorRole {
    OPERATIONS(EnumSet.of(
            OperatorPermission.VERIFICATION_READ,
            OperatorPermission.VERIFICATION_WRITE
    )),
    ADMIN(EnumSet.allOf(OperatorPermission.class));

    private final Set<OperatorPermission> permissions;

    OperatorRole(Set<OperatorPermission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public Set<OperatorPermission> permissions() {
        return permissions;
    }
}
