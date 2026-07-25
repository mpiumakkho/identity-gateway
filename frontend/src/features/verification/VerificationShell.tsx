import { useEffect, useState, type FormEvent } from "react";
import { ApiError, getJson, postJson } from "../../api/client";
import { AccountSecurityPanel } from "../auth/AccountSecurityPanel";
import { AuditInquiryPanel } from "../audit/AuditInquiryPanel";
import { AuditTimelinePanel } from "./AuditTimelinePanel";
import { OperatorManagementPanel } from "../operators/OperatorManagementPanel";
import type { AuthSession } from "../auth/types";
import { DipChipPanel } from "./DipChipPanel";
import { DopaPanel } from "./DopaPanel";
import { ManualIdentityPanel } from "./ManualIdentityPanel";
import { MethodCatalogPanel } from "./MethodCatalogPanel";
import { OperationsDashboardPanel } from "./OperationsDashboardPanel";
import { SummaryPanel } from "./SummaryPanel";
import { SystemHealthPanel } from "./SystemHealthPanel";
import type { MethodId, VerificationMethodOption, VerificationSession } from "./types";

const defaultMethods: VerificationMethodOption[] = [
  { id: "DIP_CHIP", label: "Dip Chip", description: "Card reader", enabled: true },
  { id: "MANUAL_ENTRY", label: "Manual Entry", description: "Controlled form", enabled: true }
];

const statusFilters = ["CREATED", "IDENTITY_CAPTURED", "DOPA_VERIFIED", "DOPA_REJECTED", "APPROVED", "REJECTED"];

const workflowSteps = ["Method", "Identity", "DOPA", "Summary"];
const transactionIdPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

type VerificationShellProps = {
  operator: AuthSession;
  onSessionExpired: () => void;
  onSignOut: () => void;
};

export function VerificationShell({ operator, onSessionExpired, onSignOut }: VerificationShellProps) {
  const [selectedMethod, setSelectedMethod] = useState<MethodId>("DIP_CHIP");
  const [sessionMethodFilter, setSessionMethodFilter] = useState<"ALL" | MethodId>("ALL");
  const [sessionStatusFilter, setSessionStatusFilter] = useState("ALL");
  const [methodCatalog, setMethodCatalog] = useState<VerificationMethodOption[]>(defaultMethods);
  const [sessions, setSessions] = useState<VerificationSession[]>([]);
  const [activeSession, setActiveSession] = useState<VerificationSession | null>(null);
  const [error, setError] = useState("");
  const [isStarting, setIsStarting] = useState(false);
  const [isLoadingSessions, setIsLoadingSessions] = useState(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [transactionLookup, setTransactionLookup] = useState("");
  const [isLookingUpTransaction, setIsLookingUpTransaction] = useState(false);
  const activeWorkflowIndex = workflowIndex(activeSession?.status);
  const operatorInitials = (operator.displayName || operator.username).slice(0, 2).toUpperCase();
  const navigationItems = operator.role === "ADMIN" ? ["Verification", "Transactions", "Methods", "Account", "Audit", "Operators"] : ["Verification", "Transactions", "Account"];
  const methods = methodCatalog.filter((method) => method.enabled);
  const methodLabel = (method: MethodId) => methodCatalog.find((item) => item.id === method)?.label ?? method;

  function applyMethodCatalog(nextCatalog: VerificationMethodOption[]) {
    const enabledMethods = nextCatalog.filter((method) => method.enabled);
    setMethodCatalog(nextCatalog);
    setSelectedMethod((current) => (enabledMethods.some((method) => method.id === current) ? current : enabledMethods[0]?.id ?? current));
  }

  useEffect(() => {
    let cancelled = false;

    async function loadMethodCatalog() {
      try {
        const response = await getJson<VerificationMethodOption[]>("/api/verification/methods", {
          accessToken: operator.accessToken
        });

        if (!cancelled) {
          applyMethodCatalog(response.data ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          handleApiError(err, "Unable to load verification methods.");
        }
      }
    }

    void loadMethodCatalog();

    return () => {
      cancelled = true;
    };
  }, [operator.accessToken]);

  useEffect(() => {
    let cancelled = false;

    async function loadRecentSessions() {
      setIsLoadingSessions(true);
      setError("");

      try {
        const params = new URLSearchParams();

        if (sessionMethodFilter !== "ALL") {
          params.set("method", sessionMethodFilter);
        }

        if (sessionStatusFilter !== "ALL") {
          params.set("status", sessionStatusFilter);
        }

        const path = params.size > 0 ? `/api/verification/sessions?${params.toString()}` : "/api/verification/sessions";
        const response = await getJson<VerificationSession[]>(path, {
          accessToken: operator.accessToken
        });

        if (!cancelled) {
          const nextSessions = response.data ?? [];
          setSessions(nextSessions);
          setActiveSession((current) => {
            if (current && nextSessions.some((session) => session.transactionId === current.transactionId)) {
              return current;
            }

            return nextSessions[0] ?? null;
          });
        }
      } catch (err) {
        if (!cancelled) {
          handleApiError(err, "Unable to load verification sessions.");
        }
      } finally {
        if (!cancelled) {
          setIsLoadingSessions(false);
        }
      }
    }

    void loadRecentSessions();

    return () => {
      cancelled = true;
    };
  }, [operator.accessToken, sessionMethodFilter, sessionStatusFilter]);

  function handleApiError(err: unknown, fallback: string) {
    if (err instanceof ApiError && err.status === 401) {
      onSessionExpired();
      return;
    }

    setError(err instanceof Error ? err.message : fallback);
  }

  function mergeSession(nextSession: VerificationSession) {
    setActiveSession(nextSession);
    setSessions((current) => {
      const exists = current.some((session) => session.transactionId === nextSession.transactionId);

      if (!exists) {
        return [nextSession, ...current];
      }

      return current.map((session) => (session.transactionId === nextSession.transactionId ? nextSession : session));
    });
  }

  async function startSession() {
    setIsStarting(true);
    setError("");

    try {
      const response = await postJson<VerificationSession>(
        "/api/verification/sessions",
        { method: selectedMethod },
        { accessToken: operator.accessToken }
      );

      if (response.data) {
        setSessionMethodFilter("ALL");
        setSessionStatusFilter("ALL");
        mergeSession(response.data);
      }
    } catch (err) {
      handleApiError(err, "Unable to start verification session.");
    } finally {
      setIsStarting(false);
    }
  }

  async function openSession(transactionId: string) {
    setIsLoadingDetail(true);
    setError("");

    try {
      const response = await getJson<VerificationSession>(`/api/verification/sessions/${transactionId}`, {
        accessToken: operator.accessToken
      });

      if (response.data) {
        mergeSession(response.data);
        return true;
      }

      return false;
    } catch (err) {
      handleApiError(err, "Unable to load verification session.");
      return false;
    } finally {
      setIsLoadingDetail(false);
    }
  }

  async function lookupTransaction(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const transactionId = transactionLookup.trim();

    if (!transactionId) {
      setError("Transaction ID is required.");
      return;
    }

    if (!transactionIdPattern.test(transactionId)) {
      setError("Transaction ID must be a valid UUID.");
      return;
    }

    setIsLookingUpTransaction(true);

    try {
      const loaded = await openSession(transactionId);

      if (loaded) {
        setTransactionLookup("");
      }
    } finally {
      setIsLookingUpTransaction(false);
    }
  }

  return (
    <main className="min-h-screen bg-slate-50 text-slate-950">
      <div className="grid min-h-screen grid-cols-1 lg:grid-cols-[248px_1fr]">
        <aside className="flex flex-col gap-8 border-b border-slate-200 bg-slate-950 px-5 py-5 text-white lg:border-b-0 lg:border-r lg:border-slate-900 lg:py-7">
          <div className="flex items-center justify-between gap-4 lg:block">
            <div className="flex items-center gap-3">
              <div className="grid size-11 place-items-center rounded-lg bg-teal-400 text-sm font-black text-slate-950 shadow-sm shadow-teal-950/20">
                IG
              </div>
              <div className="lg:mt-4">
                <p className="text-sm font-semibold text-white">Identity Gateway</p>
                <p className="text-xs text-slate-400">Operator Console</p>
              </div>
            </div>
          </div>

          <nav className="flex gap-2 overflow-x-auto lg:grid lg:overflow-visible" aria-label="Primary navigation">
            {navigationItems.map((label) => {
              const state = label === "Verification" ? "active" : "";

              return (
                <a
                  key={label}
                  className={
                    state === "active"
                      ? "rounded-lg bg-white/10 px-3 py-2 text-sm font-semibold text-white"
                      : "rounded-lg px-3 py-2 text-sm font-medium text-slate-400 transition hover:bg-white/10 hover:text-white"
                  }
                  href={`#${label.toLowerCase()}`}
                >
                  {label}
                </a>
              );
            })}
          </nav>
        </aside>

        <section className="px-4 py-6 sm:px-6 lg:px-8 lg:py-8" id="verification">
          <header className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-3xl font-bold tracking-normal text-slate-950">Verification Operations</h1>
              <p className="mt-1 text-sm text-slate-500">Run identity checks flow by flow with a persisted transaction trail.</p>
            </div>

            <div className="hs-dropdown relative inline-flex self-start sm:self-auto">
              <button
                id="operator-menu"
                type="button"
                className="hs-dropdown-toggle inline-flex items-center gap-x-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-teal-500"
                aria-haspopup="menu"
                aria-expanded="false"
                aria-label="Operator menu"
              >
                <span className="grid size-7 place-items-center rounded-md bg-teal-100 text-xs font-black text-teal-800">{operatorInitials}</span>
                {operator.displayName}
                <svg className="size-4 text-slate-400 hs-dropdown-open:rotate-180" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>

              <div
                className="hs-dropdown-menu z-20 mt-2 hidden min-w-48 rounded-lg border border-slate-200 bg-white p-1 opacity-0 shadow-xl shadow-slate-900/10 transition-[opacity,margin] hs-dropdown-open:opacity-100"
                role="menu"
                aria-labelledby="operator-menu"
              >
                <button className="flex w-full items-center rounded-md px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100" type="button">
                  Profile
                </button>
                <button className="flex w-full items-center rounded-md px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100" type="button" onClick={onSignOut}>
                  Sign out
                </button>
              </div>
            </div>
          </header>

          <SystemHealthPanel accessToken={operator.accessToken} onError={setError} onSessionExpired={onSessionExpired} />

          <OperationsDashboardPanel accessToken={operator.accessToken} onError={setError} onSessionExpired={onSessionExpired} />

          <div className="mb-5 grid grid-cols-2 gap-2 rounded-lg border border-slate-200 bg-white p-2 shadow-sm sm:grid-cols-4">
            {workflowSteps.map((step, index) => {
              const active = activeSession ? index <= activeWorkflowIndex : index === 0;

              return (
                <div key={step} className="flex items-center gap-2 rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-600">
                  <span className={active ? "grid size-6 place-items-center rounded-md bg-teal-600 text-xs font-bold text-white" : "grid size-6 place-items-center rounded-md bg-slate-200 text-xs font-bold text-slate-600"}>
                    {index + 1}
                  </span>
                  <span className="font-medium">{step}</span>
                </div>
              );
            })}
          </div>

          {error ? (
            <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700" role="alert">
              {error}
            </div>
          ) : null}

          <div className="grid gap-5 xl:grid-cols-[minmax(320px,500px)_1fr]">
            <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="method-title">
              <div className="mb-5 flex gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-teal-50 text-sm font-black text-teal-700">01</span>
                <div>
                  <h2 id="method-title" className="text-xl font-bold text-slate-950">Verification Method</h2>
                  <p className="mt-1 text-sm text-slate-500">Select the intake path for this transaction.</p>
                </div>
              </div>

              <div className="grid gap-3">
                {methods.length === 0 ? (
                  <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-6 text-center text-sm font-medium text-slate-500">No enabled methods.</div>
                ) : null}
                {methods.map((method) => {
                  const selected = method.id === selectedMethod;

                  return (
                    <button
                      key={method.id}
                      className={
                        selected
                          ? "flex min-h-16 w-full items-center justify-between gap-4 rounded-lg border border-teal-500 bg-teal-50 px-4 py-3 text-left shadow-[inset_3px_0_0_#0d9488] transition"
                          : "flex min-h-16 w-full items-center justify-between gap-4 rounded-lg border border-slate-200 bg-white px-4 py-3 text-left transition hover:border-slate-300 hover:bg-slate-50"
                      }
                      type="button"
                      onClick={() => setSelectedMethod(method.id)}
                    >
                      <span className="grid gap-1">
                        <span className="font-semibold text-slate-950">{method.label}</span>
                        <span className="text-sm text-slate-500">{method.description}</span>
                      </span>
                      <span className={selected ? "grid size-5 place-items-center rounded-full bg-teal-600" : "size-5 rounded-full border border-slate-300"}>
                        {selected ? <span className="size-2 rounded-full bg-white" /> : null}
                      </span>
                    </button>
                  );
                })}
              </div>

              <button
                className="mt-5 inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                type="button"
                onClick={startSession}
                disabled={isStarting || methods.length === 0}
              >
                {isStarting ? "Starting..." : "Start Session"}
              </button>
            </section>

            <div className="grid gap-5">
              <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="status-title">
                <div className="mb-5 flex items-start justify-between gap-3">
                  <div className="flex gap-3">
                    <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-slate-100 text-sm font-black text-slate-700">02</span>
                    <div>
                      <h2 id="status-title" className="text-xl font-bold text-slate-950">Session Detail</h2>
                      <p className="mt-1 text-sm text-slate-500">Current transaction state.</p>
                    </div>
                  </div>
                  {isLoadingDetail ? <span className="text-xs font-semibold text-slate-400">Loading</span> : null}
                </div>

                {activeSession ? (
                  <dl className="grid gap-4">
                    <div className="border-b border-slate-100 pb-4">
                      <dt className="text-xs font-bold uppercase text-slate-400">Transaction ID</dt>
                      <dd className="mt-1 break-all text-sm font-semibold text-slate-950">{activeSession.transactionId}</dd>
                    </div>
                    <div className="grid gap-4 border-b border-slate-100 pb-4 sm:grid-cols-3">
                      <div>
                        <dt className="text-xs font-bold uppercase text-slate-400">Method</dt>
                        <dd className="mt-1 text-sm font-semibold text-slate-950">{methodLabel(activeSession.method)}</dd>
                      </div>
                      <div>
                        <dt className="text-xs font-bold uppercase text-slate-400">Status</dt>
                        <dd className={`mt-1 text-sm font-semibold ${statusClassName(activeSession.status)}`}>{activeSession.status}</dd>
                      </div>
                      <div>
                        <dt className="text-xs font-bold uppercase text-slate-400">Created</dt>
                        <dd className="mt-1 text-sm font-semibold text-slate-950">{new Date(activeSession.createdAt).toLocaleString()}</dd>
                      </div>
                    </div>
                    <div>
                      <dt className="text-xs font-bold uppercase text-slate-400">Created By</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-950">{activeSession.createdBy?.displayName ?? "Unknown operator"}</dd>
                    </div>

                    {activeSession.identity ? (
                      <div className="border-t border-slate-100 pt-4">
                        <dt className="text-xs font-bold uppercase text-slate-400">Identity Summary</dt>
                        <dd className="mt-3 grid gap-3 sm:grid-cols-3">
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Citizen</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">
                              {activeSession.identity.title} {activeSession.identity.firstName} {activeSession.identity.lastName}
                            </span>
                          </span>
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">National ID</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{activeSession.identity.maskedNationalId}</span>
                          </span>
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Birth Date</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{formatDate(activeSession.identity.dateOfBirth)}</span>
                          </span>
                          {activeSession.identity.readerName ? (
                            <span>
                              <span className="block text-xs font-semibold text-slate-400">Reader</span>
                              <span className="mt-1 block text-sm font-semibold text-slate-950">{activeSession.identity.readerName}</span>
                            </span>
                          ) : null}
                          {activeSession.identity.readerSerialNumber ? (
                            <span>
                              <span className="block text-xs font-semibold text-slate-400">Reader Serial</span>
                              <span className="mt-1 block text-sm font-semibold text-slate-950">{activeSession.identity.readerSerialNumber}</span>
                            </span>
                          ) : null}
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Updated</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{formatDateTime(activeSession.identity.updatedAt)}</span>
                          </span>
                        </dd>
                      </div>
                    ) : null}

                    {activeSession.dopaValidation ? (
                      <div className="border-t border-slate-100 pt-4">
                        <dt className="text-xs font-bold uppercase text-slate-400">DOPA Result</dt>
                        <dd className="mt-3 grid gap-3 sm:grid-cols-3">
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Status</span>
                            <span className={`mt-1 block text-sm font-semibold ${activeSession.dopaValidation.validationStatus === "MATCHED" ? "text-emerald-700" : "text-red-700"}`}>
                              {activeSession.dopaValidation.validationStatus}
                            </span>
                          </span>
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Code</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{activeSession.dopaValidation.responseCode}</span>
                          </span>
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Validated</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{formatDateTime(activeSession.dopaValidation.validatedAt)}</span>
                          </span>
                          <span className="sm:col-span-3">
                            <span className="block text-xs font-semibold text-slate-400">Message</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{activeSession.dopaValidation.responseMessage}</span>
                          </span>
                        </dd>
                      </div>
                    ) : null}

                    {activeSession.closeout ? (
                      <div className="border-t border-slate-100 pt-4">
                        <dt className="text-xs font-bold uppercase text-slate-400">Closeout</dt>
                        <dd className="mt-3 grid gap-3 sm:grid-cols-3">
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Decision</span>
                            <span className={`mt-1 block text-sm font-semibold ${statusClassName(activeSession.closeout.decision)}`}>{activeSession.closeout.decision}</span>
                          </span>
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Decided By</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{activeSession.closeout.decidedBy.displayName}</span>
                          </span>
                          <span>
                            <span className="block text-xs font-semibold text-slate-400">Decided</span>
                            <span className="mt-1 block text-sm font-semibold text-slate-950">{formatDateTime(activeSession.closeout.decidedAt)}</span>
                          </span>
                          {activeSession.closeout.notes ? (
                            <span className="sm:col-span-3">
                              <span className="block text-xs font-semibold text-slate-400">Notes</span>
                              <span className="mt-1 block text-sm font-semibold text-slate-950">{activeSession.closeout.notes}</span>
                            </span>
                          ) : null}
                        </dd>
                      </div>
                    ) : null}
                  </dl>
                ) : (
                  <div className="grid min-h-44 place-items-center rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 text-center">
                    <div>
                      <strong className="block text-sm font-bold text-slate-800">No active session</strong>
                      <span className="mt-1 block text-sm text-slate-500">Choose a method to create the first transaction.</span>
                    </div>
                  </div>
                )}
              </section>

              {activeSession?.method === "DIP_CHIP" || (!activeSession && selectedMethod === "DIP_CHIP") ? (
                <DipChipPanel
                  accessToken={operator.accessToken}
                  session={activeSession}
                  onError={setError}
                  onSaved={mergeSession}
                  onSessionExpired={onSessionExpired}
                />
              ) : (
                <ManualIdentityPanel
                  accessToken={operator.accessToken}
                  session={activeSession}
                  onError={setError}
                  onSaved={mergeSession}
                  onSessionExpired={onSessionExpired}
                />
              )}

              <DopaPanel
                accessToken={operator.accessToken}
                session={activeSession}
                onError={setError}
                onSaved={mergeSession}
                onSessionExpired={onSessionExpired}
              />

              <SummaryPanel
                accessToken={operator.accessToken}
                session={activeSession}
                onError={setError}
                onSaved={mergeSession}
                onSessionExpired={onSessionExpired}
              />

              <AuditTimelinePanel
                accessToken={operator.accessToken}
                session={activeSession}
                onError={setError}
                onSessionExpired={onSessionExpired}
              />
            </div>
          </div>

          <section className="mt-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" id="transactions" aria-labelledby="transactions-title">
            <div className="mb-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <h2 id="transactions-title" className="text-xl font-bold text-slate-950">Recent Transactions</h2>
                <p className="mt-1 text-sm text-slate-500">Latest persisted verification sessions.</p>
              </div>
              <div className="grid gap-3">
                <form className="grid gap-2 sm:grid-cols-[minmax(240px,360px)_auto] sm:items-end" onSubmit={lookupTransaction}>
                  <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
                    Transaction ID
                    <input
                      className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                      placeholder="00000000-0000-0000-0000-000000000000"
                      value={transactionLookup}
                      onChange={(event) => setTransactionLookup(event.target.value)}
                    />
                  </label>
                  <button
                    className="min-h-10 rounded-lg bg-slate-950 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
                    type="submit"
                    disabled={isLookingUpTransaction || isLoadingDetail}
                  >
                    {isLookingUpTransaction ? "Loading..." : "Lookup"}
                  </button>
                </form>
                <div className="grid gap-3 sm:grid-cols-[180px_220px_auto] sm:items-end">
                  <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
                    Method
                    <select
                      className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                      value={sessionMethodFilter}
                      onChange={(event) => setSessionMethodFilter(event.target.value as "ALL" | MethodId)}
                    >
                      <option value="ALL">All methods</option>
                      {methods.map((method) => (
                        <option key={method.id} value={method.id}>
                          {method.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
                    Status
                    <select
                      className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                      value={sessionStatusFilter}
                      onChange={(event) => setSessionStatusFilter(event.target.value)}
                    >
                      <option value="ALL">All statuses</option>
                      {statusFilters.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </label>
                  <span className="rounded-md bg-slate-100 px-2.5 py-2 text-center text-xs font-bold text-slate-600">{sessions.length}</span>
                </div>
              </div>
            </div>

            {isLoadingSessions ? (
              <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">Loading sessions...</div>
            ) : sessions.length > 0 ? (
              <div className="overflow-hidden rounded-lg border border-slate-200">
                <div className="grid grid-cols-[1fr_120px_150px] bg-slate-50 px-4 py-2 text-xs font-bold uppercase text-slate-400 max-md:hidden">
                  <span>Transaction</span>
                  <span>Status</span>
                  <span>Created</span>
                </div>
                <div className="divide-y divide-slate-100">
                  {sessions.map((session) => {
                    const selected = activeSession?.transactionId === session.transactionId;

                    return (
                      <button
                        key={session.transactionId}
                        className={
                          selected
                            ? "grid w-full gap-2 bg-teal-50 px-4 py-3 text-left md:grid-cols-[1fr_120px_150px]"
                            : "grid w-full gap-2 bg-white px-4 py-3 text-left transition hover:bg-slate-50 md:grid-cols-[1fr_120px_150px]"
                        }
                        type="button"
                        onClick={() => void openSession(session.transactionId)}
                      >
                        <span className="min-w-0">
                          <span className="block truncate text-sm font-bold text-slate-950">{methodLabel(session.method)}</span>
                          <span className="mt-1 block truncate text-xs text-slate-500">{session.transactionId}</span>
                        </span>
                        <span className={`text-sm font-semibold ${statusClassName(session.status)}`}>{session.status}</span>
                        <span className="text-sm text-slate-500">{new Date(session.createdAt).toLocaleDateString()}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : (
              <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">No transactions yet.</div>
            )}
          </section>

          {operator.role === "ADMIN" ? (
            <MethodCatalogPanel accessToken={operator.accessToken} onCatalogChanged={applyMethodCatalog} onError={setError} onSessionExpired={onSessionExpired} />
          ) : null}

          <AccountSecurityPanel
            accessToken={operator.accessToken}
            onError={setError}
            onSessionExpired={onSessionExpired}
          />

          {operator.role === "ADMIN" ? (
            <AuditInquiryPanel accessToken={operator.accessToken} onError={setError} onSessionExpired={onSessionExpired} />
          ) : null}

          {operator.role === "ADMIN" ? (
            <OperatorManagementPanel
              accessToken={operator.accessToken}
              currentOperatorId={operator.operatorId}
              onError={setError}
              onSessionExpired={onSessionExpired}
            />
          ) : null}
        </section>
      </div>
    </main>
  );
}

function formatDate(value?: string | null) {
  if (!value) {
    return "-";
  }

  return new Date(value).toLocaleDateString();
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }

  return new Date(value).toLocaleString();
}

function statusClassName(status: string) {
  if (status === "DOPA_VERIFIED" || status === "APPROVED") {
    return "text-emerald-700";
  }

  if (status === "DOPA_REJECTED" || status === "REJECTED") {
    return "text-red-700";
  }

  return status === "IDENTITY_CAPTURED" ? "text-cyan-700" : "text-teal-700";
}

function workflowIndex(status?: string) {
  if (status === "APPROVED" || status === "REJECTED") {
    return 3;
  }

  if (status === "DOPA_VERIFIED" || status === "DOPA_REJECTED") {
    return 2;
  }

  if (status === "IDENTITY_CAPTURED") {
    return 1;
  }

  return 0;
}
