import type { AuthSession } from "./types";

const storageKey = "identity-gateway.auth-session";

function isAuthSession(value: unknown): value is AuthSession {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<AuthSession>;
  return (
    typeof candidate.operatorId === "string" &&
    typeof candidate.username === "string" &&
    typeof candidate.displayName === "string" &&
    typeof candidate.role === "string" &&
    typeof candidate.authenticatedAt === "string" &&
    typeof candidate.accessToken === "string" &&
    typeof candidate.expiresAt === "string"
  );
}

export function loadAuthSession(): AuthSession | null {
  const serialized = sessionStorage.getItem(storageKey);

  if (!serialized) {
    return null;
  }

  try {
    const parsed = JSON.parse(serialized) as unknown;

    if (!isAuthSession(parsed) || new Date(parsed.expiresAt).getTime() <= Date.now()) {
      clearAuthSession();
      return null;
    }

    return parsed;
  } catch {
    clearAuthSession();
    return null;
  }
}

export function saveAuthSession(session: AuthSession) {
  sessionStorage.setItem(storageKey, JSON.stringify(session));
}

export function clearAuthSession() {
  sessionStorage.removeItem(storageKey);
}