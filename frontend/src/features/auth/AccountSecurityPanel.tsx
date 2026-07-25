import { useEffect, useState, type FormEvent } from "react";
import { ApiError, deleteJson, getJson, putJson } from "../../api/client";

type AccountSecurityPanelProps = {
  accessToken: string;
  onError: (message: string) => void;
  onSessionExpired: () => void;
};

type PasswordForm = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

type OperatorSession = {
  sessionId: string;
  current: boolean;
  createdAt: string;
  expiresAt: string;
};

const emptyPasswordForm: PasswordForm = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: ""
};

export function AccountSecurityPanel({ accessToken, onError, onSessionExpired }: AccountSecurityPanelProps) {
  const [form, setForm] = useState<PasswordForm>(emptyPasswordForm);
  const [sessions, setSessions] = useState<OperatorSession[]>([]);
  const [successMessage, setSuccessMessage] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isLoadingSessions, setIsLoadingSessions] = useState(true);
  const [revokingSessionId, setRevokingSessionId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadSessions() {
      setIsLoadingSessions(true);
      onError("");

      try {
        const response = await getJson<OperatorSession[]>("/api/auth/sessions", { accessToken });

        if (!cancelled) {
          setSessions(response.data ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          handleApiError(err, "Unable to load active sessions.");
        }
      } finally {
        if (!cancelled) {
          setIsLoadingSessions(false);
        }
      }
    }

    void loadSessions();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  function handleApiError(err: unknown, fallback: string) {
    if (err instanceof ApiError && err.status === 401 && err.code === "AUTHENTICATION_REQUIRED") {
      onSessionExpired();
      return;
    }

    onError(err instanceof Error ? err.message : fallback);
  }

  async function refreshSessions() {
    const response = await getJson<OperatorSession[]>("/api/auth/sessions", { accessToken });
    setSessions(response.data ?? []);
  }

  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSuccessMessage("");
    onError("");

    if (form.newPassword !== form.confirmPassword) {
      onError("New password confirmation does not match.");
      return;
    }

    setIsSaving(true);

    try {
      await putJson<{ passwordChanged: boolean }>(
        "/api/auth/password",
        {
          currentPassword: form.currentPassword,
          newPassword: form.newPassword
        },
        { accessToken }
      );
      setForm(emptyPasswordForm);
      setSuccessMessage("Password changed.");
      await refreshSessions();
    } catch (err) {
      handleApiError(err, "Unable to change password.");
    } finally {
      setIsSaving(false);
    }
  }

  async function revokeSession(sessionId: string) {
    setRevokingSessionId(sessionId);
    setSuccessMessage("");
    onError("");

    try {
      await deleteJson<{ revoked: boolean }>(`/api/auth/sessions/${sessionId}`, { accessToken });
      setSessions((current) => current.filter((session) => session.sessionId !== sessionId));
      setSuccessMessage("Session revoked.");
    } catch (err) {
      handleApiError(err, "Unable to revoke session.");
    } finally {
      setRevokingSessionId(null);
    }
  }

  return (
    <section className="mt-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" id="account" aria-labelledby="account-title">
      <div className="mb-5">
        <h2 id="account-title" className="text-xl font-bold text-slate-950">Account Security</h2>
        <p className="mt-1 text-sm text-slate-500">Manage your password and active sessions.</p>
      </div>

      <form className="grid gap-3 lg:grid-cols-[1fr_1fr_1fr_auto] lg:items-end" onSubmit={changePassword}>
        <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
          Current Password
          <input
            className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
            type="password"
            value={form.currentPassword}
            onChange={(event) => setForm((current) => ({ ...current, currentPassword: event.target.value }))}
            required
            maxLength={128}
            autoComplete="current-password"
          />
        </label>
        <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
          New Password
          <input
            className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
            type="password"
            value={form.newPassword}
            onChange={(event) => setForm((current) => ({ ...current, newPassword: event.target.value }))}
            required
            minLength={12}
            maxLength={128}
            autoComplete="new-password"
          />
        </label>
        <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
          Confirm Password
          <input
            className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
            type="password"
            value={form.confirmPassword}
            onChange={(event) => setForm((current) => ({ ...current, confirmPassword: event.target.value }))}
            required
            minLength={12}
            maxLength={128}
            autoComplete="new-password"
          />
        </label>
        <button
          className="min-h-10 rounded-lg bg-teal-600 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-teal-700 disabled:cursor-not-allowed disabled:bg-slate-300"
          type="submit"
          disabled={isSaving}
        >
          {isSaving ? "Saving..." : "Change"}
        </button>
      </form>

      {successMessage ? (
        <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700" role="status">
          {successMessage}
        </div>
      ) : null}

      <div className="mt-6 border-t border-slate-100 pt-5">
        <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-950">Active Sessions</h3>
            <p className="mt-1 text-sm text-slate-500">Review signed-in sessions for this account.</p>
          </div>
          <span className="self-start rounded-md bg-slate-100 px-2.5 py-2 text-xs font-bold text-slate-600 sm:self-auto">{sessions.length}</span>
        </div>

        {isLoadingSessions ? (
          <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">Loading sessions...</div>
        ) : sessions.length > 0 ? (
          <div className="overflow-hidden rounded-lg border border-slate-200">
            <div className="grid grid-cols-[1fr_150px_140px] bg-slate-50 px-4 py-2 text-xs font-bold uppercase text-slate-400 max-md:hidden">
              <span>Session</span>
              <span>Expires</span>
              <span>Control</span>
            </div>
            <div className="divide-y divide-slate-100">
              {sessions.map((session) => (
                <div key={session.sessionId} className="grid gap-3 px-4 py-4 md:grid-cols-[1fr_150px_140px] md:items-center">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <strong className="text-sm text-slate-950">{session.current ? "Current session" : "Signed-in session"}</strong>
                      {session.current ? <span className="rounded-md bg-teal-50 px-2 py-1 text-xs font-bold text-teal-700">Current</span> : null}
                    </div>
                    <span className="mt-1 block truncate text-xs text-slate-500">Started {formatDateTime(session.createdAt)}</span>
                  </div>
                  <span className="text-sm font-semibold text-slate-700">{formatDateTime(session.expiresAt)}</span>
                  <button
                    className="min-h-10 rounded-lg border border-red-200 bg-white px-3 text-sm font-bold text-red-700 shadow-sm transition hover:bg-red-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                    type="button"
                    onClick={() => void revokeSession(session.sessionId)}
                    disabled={session.current || revokingSessionId === session.sessionId}
                  >
                    {revokingSessionId === session.sessionId ? "Revoking..." : "Revoke"}
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">No active sessions found.</div>
        )}
      </div>
    </section>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString();
}
