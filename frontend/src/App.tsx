import { useEffect, useState } from "react";
import { postJson } from "./api/client";
import { LoginScreen } from "./features/auth/LoginScreen";
import { clearAuthSession, loadAuthSession, saveAuthSession } from "./features/auth/authStorage";
import type { AuthSession } from "./features/auth/types";
import { VerificationShell } from "./features/verification/VerificationShell";
import { usePreline } from "./lib/usePreline";

const sessionExpiredMessage = "Your session has expired. Please sign in again.";

export default function App() {
  usePreline();
  const [authSession, setAuthSession] = useState<AuthSession | null>(() => loadAuthSession());
  const [authNotice, setAuthNotice] = useState("");

  useEffect(() => {
    if (!authSession) {
      return undefined;
    }

    const expiresInMs = new Date(authSession.expiresAt).getTime() - Date.now();

    if (expiresInMs <= 0) {
      clearSession(sessionExpiredMessage);
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      clearSession(sessionExpiredMessage);
    }, expiresInMs);

    return () => window.clearTimeout(timeoutId);
  }, [authSession]);

  function handleAuthenticated(session: AuthSession) {
    saveAuthSession(session);
    setAuthNotice("");
    setAuthSession(session);
  }

  function clearSession(notice = "") {
    clearAuthSession();
    setAuthNotice(notice);
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
    <VerificationShell operator={authSession} onSessionExpired={() => clearSession(sessionExpiredMessage)} onSignOut={handleSignOut} />
  ) : (
    <LoginScreen notice={authNotice} onAuthenticated={handleAuthenticated} />
  );
}
