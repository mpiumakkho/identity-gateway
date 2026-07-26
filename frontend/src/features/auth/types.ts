export type OperatorRole = "OPERATIONS" | "ADMIN";

export type OperatorPermission =
  | "VERIFICATION_READ"
  | "VERIFICATION_WRITE"
  | "METHOD_CATALOG_MANAGE"
  | "AUDIT_READ"
  | "OPERATOR_MANAGE";

export type AuthSession = {
  operatorId: string;
  username: string;
  displayName: string;
  role: OperatorRole;
  permissions: OperatorPermission[];
  authenticatedAt: string;
  accessToken: string;
  expiresAt: string;
};

export type LoginCredentials = {
  username: string;
  password: string;
};
