import type { AuthSession, OperatorPermission, OperatorRole } from "./types";

const permissionsByRole: Record<OperatorRole, OperatorPermission[]> = {
  OPERATIONS: ["VERIFICATION_READ", "VERIFICATION_WRITE"],
  ADMIN: ["VERIFICATION_READ", "VERIFICATION_WRITE", "METHOD_CATALOG_MANAGE", "AUDIT_READ", "OPERATOR_MANAGE"]
};

export function permissionsForRole(role: OperatorRole): OperatorPermission[] {
  return permissionsByRole[role] ?? [];
}

export function normalizePermissions(session: AuthSession): AuthSession {
  return {
    ...session,
    permissions: session.permissions?.length ? session.permissions : permissionsForRole(session.role)
  };
}

export function hasPermission(session: AuthSession, permission: OperatorPermission) {
  return normalizePermissions(session).permissions.includes(permission);
}
