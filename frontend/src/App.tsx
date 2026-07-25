import { useState } from "react";
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

  function handleSignOut() {
    clearAuthSession();
    setAuthSession(null);
  }

  return authSession ? (
    <VerificationShell operator={authSession} onSignOut={handleSignOut} />
  ) : (
    <LoginScreen onAuthenticated={handleAuthenticated} />
  );
}