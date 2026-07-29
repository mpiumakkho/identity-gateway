import { useEffect, useState, type FormEvent } from "react";
import { deleteJson, getJson, postJson, putJson } from "../../api/client";
import { handleApiFailure } from "../../api/errors";
import type { OperatorRole } from "../auth/types";
import type { CreateOperatorPayload, OperatorSession, OperatorUser, SessionRevocationSummary } from "./types";

type OperatorManagementPanelProps = {
  accessToken: string;
  currentOperatorId: string;
  onError: (message: string) => void;
  onSessionExpired: () => void;
};

const emptyCreateForm: CreateOperatorPayload = {
  username: "",
  displayName: "",
  password: "",
  role: "OPERATIONS"
};

export function OperatorManagementPanel({ accessToken, currentOperatorId, onError, onSessionExpired }: OperatorManagementPanelProps) {
  const [operators, setOperators] = useState<OperatorUser[]>([]);
  const [createForm, setCreateForm] = useState<CreateOperatorPayload>(emptyCreateForm);
  const [passwordByOperatorId, setPasswordByOperatorId] = useState<Record<string, string>>({});
  const [sessionsByOperatorId, setSessionsByOperatorId] = useState<Record<string, OperatorSession[]>>({});
  const [expandedSessionOperatorId, setExpandedSessionOperatorId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [busyOperatorId, setBusyOperatorId] = useState<string | null>(null);
  const [busySessionId, setBusySessionId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadOperators() {
      setIsLoading(true);
      onError("");

      try {
        const response = await getJson<OperatorUser[]>("/api/operators", { accessToken });

        if (!cancelled) {
          setOperators(response.data ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          handleApiError(err, "Unable to load operators.");
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadOperators();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  function handleApiError(err: unknown, fallback: string) {
    handleApiFailure(err, fallback, onSessionExpired, onError);
  }

  async function refreshOperators() {
    const response = await getJson<OperatorUser[]>("/api/operators", { accessToken });
    setOperators(response.data ?? []);
  }

  async function createOperator(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsCreating(true);
    onError("");

    try {
      const response = await postJson<OperatorUser>("/api/operators", createForm, { accessToken });
      setCreateForm(emptyCreateForm);

      if (response.data) {
        setOperators((current) => [response.data as OperatorUser, ...current]);
      } else {
        await refreshOperators();
      }
    } catch (err) {
      handleApiError(err, "Unable to create operator.");
    } finally {
      setIsCreating(false);
    }
  }

  async function changePassword(operatorId: string) {
    const password = passwordByOperatorId[operatorId]?.trim();

    if (!password) {
      onError("Password is required.");
      return;
    }

    setBusyOperatorId(operatorId);
    onError("");

    try {
      await putJson<OperatorUser>(`/api/operators/${operatorId}/password`, { password }, { accessToken });
      setPasswordByOperatorId((current) => ({ ...current, [operatorId]: "" }));
      await refreshOperators();
    } catch (err) {
      handleApiError(err, "Unable to change operator password.");
    } finally {
      setBusyOperatorId(null);
    }
  }


  async function loadOperatorSessions(operatorId: string) {
    setExpandedSessionOperatorId(operatorId);
    setBusyOperatorId(operatorId);
    onError("");

    try {
      const response = await getJson<OperatorSession[]>(`/api/operators/${operatorId}/sessions`, { accessToken });
      setSessionsByOperatorId((current) => ({ ...current, [operatorId]: response.data ?? [] }));
    } catch (err) {
      handleApiError(err, "Unable to load operator sessions.");
    } finally {
      setBusyOperatorId(null);
    }
  }

  async function revokeOperatorSession(operatorId: string, sessionId: string) {
    setBusySessionId(sessionId);
    onError("");

    try {
      await deleteJson<SessionRevocationSummary>(`/api/operators/${operatorId}/sessions/${sessionId}`, { accessToken });
      await loadOperatorSessions(operatorId);
    } catch (err) {
      handleApiError(err, "Unable to revoke operator session.");
    } finally {
      setBusySessionId(null);
    }
  }

  async function revokeOperatorSessions(operatorId: string) {
    setBusyOperatorId(operatorId);
    onError("");

    try {
      await deleteJson<SessionRevocationSummary>(`/api/operators/${operatorId}/sessions`, { accessToken });
      await loadOperatorSessions(operatorId);
    } catch (err) {
      handleApiError(err, "Unable to revoke operator sessions.");
    } finally {
      setBusyOperatorId(null);
    }
  }

  async function disableOperator(operatorId: string) {
    setBusyOperatorId(operatorId);
    onError("");

    try {
      const response = await putJson<OperatorUser>(`/api/operators/${operatorId}/disabled`, undefined, { accessToken });

      if (response.data) {
        setOperators((current) => current.map((operator) => (operator.operatorId === operatorId ? response.data as OperatorUser : operator)));
      } else {
        await refreshOperators();
      }
    } catch (err) {
      handleApiError(err, "Unable to disable operator.");
    } finally {
      setBusyOperatorId(null);
    }
  }

  return (
    <section className="mt-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" id="operators" aria-labelledby="operators-title">
      <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 id="operators-title" className="text-xl font-bold text-slate-950">Operator Management</h2>
          <p className="mt-1 text-sm text-slate-500">Create accounts and control operator access.</p>
        </div>
        <span className="self-start rounded-md bg-slate-100 px-2.5 py-2 text-xs font-bold text-slate-600 sm:self-auto">{operators.length}</span>
      </div>

      <form className="mb-5 grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 lg:grid-cols-[1fr_1fr_1fr_160px_auto] lg:items-end" onSubmit={createOperator}>
        <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
          Username
          <input
            className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
            value={createForm.username}
            onChange={(event) => setCreateForm((current) => ({ ...current, username: event.target.value }))}
            required
            minLength={3}
            maxLength={80}
            autoComplete="off"
          />
        </label>
        <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
          Display Name
          <input
            className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
            value={createForm.displayName}
            onChange={(event) => setCreateForm((current) => ({ ...current, displayName: event.target.value }))}
            required
            maxLength={120}
            autoComplete="off"
          />
        </label>
        <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
          Password
          <input
            className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
            type="password"
            value={createForm.password}
            onChange={(event) => setCreateForm((current) => ({ ...current, password: event.target.value }))}
            required
            minLength={12}
            maxLength={128}
            autoComplete="new-password"
          />
        </label>
        <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
          Role
          <select
            className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
            value={createForm.role}
            onChange={(event) => setCreateForm((current) => ({ ...current, role: event.target.value as OperatorRole }))}
          >
            <option value="OPERATIONS">Operations</option>
            <option value="ADMIN">Admin</option>
          </select>
        </label>
        <button
          className="min-h-10 rounded-lg bg-teal-600 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-teal-700 disabled:cursor-not-allowed disabled:bg-slate-300"
          type="submit"
          disabled={isCreating}
        >
          {isCreating ? "Creating..." : "Create"}
        </button>
      </form>

      {isLoading ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">Loading operators...</div>
      ) : operators.length > 0 ? (
        <div className="overflow-hidden rounded-lg border border-slate-200">
          <div className="grid grid-cols-[1fr_120px_110px_420px] bg-slate-50 px-4 py-2 text-xs font-bold uppercase text-slate-400 max-xl:hidden">
            <span>Operator</span>
            <span>Role</span>
            <span>Status</span>
            <span>Controls</span>
          </div>
          <div className="divide-y divide-slate-100">
            {operators.map((operator) => {
              const isCurrentOperator = operator.operatorId === currentOperatorId;
              const isBusy = busyOperatorId === operator.operatorId;

              return (
                <div key={operator.operatorId} className="grid gap-3 px-4 py-4 xl:grid-cols-[1fr_120px_110px_420px] xl:items-center">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <strong className="truncate text-sm text-slate-950">{operator.displayName}</strong>
                      {isCurrentOperator ? <span className="rounded-md bg-teal-50 px-2 py-1 text-xs font-bold text-teal-700">Current</span> : null}
                    </div>
                    <span className="mt-1 block truncate text-xs text-slate-500">{operator.username}</span>
                  </div>
                  <span className="text-sm font-semibold text-slate-700">{operator.role}</span>
                  <span className={operator.enabled ? "text-sm font-semibold text-emerald-700" : "text-sm font-semibold text-red-700"}>
                    {operator.enabled ? "Enabled" : "Disabled"}
                  </span>
                  <div className="grid gap-2 sm:grid-cols-[1fr_auto_auto_auto]">
                    <input
                      className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                      type="password"
                      value={passwordByOperatorId[operator.operatorId] ?? ""}
                      onChange={(event) => setPasswordByOperatorId((current) => ({ ...current, [operator.operatorId]: event.target.value }))}
                      minLength={12}
                      maxLength={128}
                      autoComplete="new-password"
                      placeholder="New password"
                    />
                    <button
                      className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
                      type="button"
                      onClick={() => void changePassword(operator.operatorId)}
                      disabled={isBusy}
                    >
                      Update
                    </button>
                    <button
                      className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
                      type="button"
                      onClick={() => void loadOperatorSessions(operator.operatorId)}
                      disabled={isBusy}
                    >
                      Sessions
                    </button>
                    <button
                      className="min-h-10 rounded-lg border border-red-200 bg-white px-3 text-sm font-bold text-red-700 shadow-sm transition hover:bg-red-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                      type="button"
                      onClick={() => void disableOperator(operator.operatorId)}
                      disabled={isBusy || isCurrentOperator || !operator.enabled}
                    >
                      Disable
                    </button>
                  </div>
                  {expandedSessionOperatorId === operator.operatorId ? (
                    <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 xl:col-span-4">
                      <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <p className="text-xs font-bold uppercase text-slate-500">Active Sessions</p>
                        <button
                          className="min-h-9 rounded-lg border border-red-200 bg-white px-3 text-xs font-bold text-red-700 shadow-sm transition hover:bg-red-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                          type="button"
                          onClick={() => void revokeOperatorSessions(operator.operatorId)}
                          disabled={isBusy || (sessionsByOperatorId[operator.operatorId] ?? []).every((session) => session.current)}
                        >
                          Revoke All
                        </button>
                      </div>
                      {(sessionsByOperatorId[operator.operatorId] ?? []).length > 0 ? (
                        <div className="grid gap-2">
                          {(sessionsByOperatorId[operator.operatorId] ?? []).map((session) => (
                            <div key={session.sessionId} className="grid gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm md:grid-cols-[1fr_1fr_auto] md:items-center">
                              <span className="font-semibold text-slate-700">{new Date(session.createdAt).toLocaleString()}</span>
                              <span className="text-slate-500">Expires {new Date(session.expiresAt).toLocaleString()}</span>
                              <button
                                className="min-h-9 rounded-lg border border-red-200 bg-white px-3 text-xs font-bold text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                                type="button"
                                onClick={() => void revokeOperatorSession(operator.operatorId, session.sessionId)}
                                disabled={session.current || busySessionId === session.sessionId}
                              >
                                {session.current ? "Current" : "Revoke"}
                              </button>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm font-medium text-slate-500">No active sessions.</p>
                      )}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">No operators found.</div>
      )}
    </section>
  );
}
