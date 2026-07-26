import { ApiError } from "./client";

export function isAuthenticationRequired(err: unknown) {
  return err instanceof ApiError && err.status === 401 && err.code === "AUTHENTICATION_REQUIRED";
}

export function messageFromError(err: unknown, fallback: string) {
  return err instanceof Error ? err.message : fallback;
}

export function handleApiFailure(
  err: unknown,
  fallback: string,
  onSessionExpired: () => void,
  onError: (message: string) => void
) {
  if (isAuthenticationRequired(err)) {
    onSessionExpired();
    return;
  }

  onError(messageFromError(err, fallback));
}
