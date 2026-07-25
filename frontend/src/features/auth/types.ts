export type OperatorRole = "OPERATIONS" | "ADMIN";

export type AuthSession = {
  operatorId: string;
  username: string;
  displayName: string;
  role: OperatorRole;
  authenticatedAt: string;
  accessToken: string;
  expiresAt: string;
};

export type LoginCredentials = {
  username: string;
  password: string;
};