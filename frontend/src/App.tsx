import { useState } from "react";
import { postJson } from "./api/client";
import { LoginScreen } from "./features/auth/LoginScreen";
import { clearAuthSession, loadAuthSession, saveAuthSession } from "./features/auth/authStorage";
import type { AuthSession } from "./features/auth/types";
import { VerificationShell } from "./features/verification/VerificationShell";
import { usePreline } from "./lib/usePreline";

export default function App() {
  usePreline();
  const [authSession, setAuthSession] = useState<AuthSession | null>(() => loadAuthSession());

  function handleAuthenticated(session: AuthSession) {
    saveAuthSession(session);
    setAuthSession(session);
  }

  function clearSession() {
    clearAuthSession();
    setAuthSession(null);
  }

  function handleSignOut() {
    const accessToken = authSession?.accessToken;
    clearSession();

    if (accessToken) {
      void postJson("/api/auth/logout", undefined, { accessToken }).catch(() => undefined);
    }
  }

  return authSession ? (
    <VerificationShell operator={authSession} onSessionExpired={clearSession} onSignOut={handleSignOut} />
  ) : (
    <LoginScreen onAuthenticated={handleAuthenticated} />
  );
}