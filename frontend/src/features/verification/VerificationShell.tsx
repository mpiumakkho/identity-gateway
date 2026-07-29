import { useEffect, useState, type FormEvent } from "react";
import { downloadFile, getJson, postJson } from "../../api/client";
import { handleApiFailure } from "../../api/errors";
import { AccountSecurityPanel } from "../auth/AccountSecurityPanel";
import { AuditInquiryPanel } from "../audit/AuditInquiryPanel";
import { AuditTimelinePanel } from "./AuditTimelinePanel";
import { OperatorManagementPanel } from "../operators/OperatorManagementPanel";
import { hasPermission } from "../auth/permissions";
import type { AuthSession } from "../auth/types";
import { DipChipPanel } from "./DipChipPanel";
import { DopaPanel } from "./DopaPanel";
import { ManualIdentityPanel } from "./ManualIdentityPanel";
import { MethodCatalogPanel } from "./MethodCatalogPanel";
import { OperationsDashboardPanel } from "./OperationsDashboardPanel";
import { SummaryPanel } from "./SummaryPanel";
import { SystemHealthPanel } from "./SystemHealthPanel";
import { nationalIdValidationMessage } from "./nationalId";
import type { MethodId, VerificationMethodOption, VerificationSession } from "./types";

const defaultMethods: VerificationMethodOption[] = [
  { id: "DIP_CHIP", label: "Dip Chip", description: "Card reader", enabled: true },
  { id: "MANUAL_ENTRY", label: "Manual Entry", description: "Controlled form", enabled: true }
];

const statusFilters = ["CREATED", "IDENTITY_CAPTURED", "DOPA_VERIFIED", "DOPA_REJECTED", "APPROVED", "REJECTED"];
const sessionLimitOptions = [20, 50, 100];

const workflowSteps = ["Method", "Identity", "DOPA", "Summary"];
const transactionIdPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const uuidPattern = transactionIdPattern;

type VerificationShellProps = {
  operator: AuthSession;
  onSessionExpired: () => void;
  onSignOut: () => void;
};

export function VerificationShell({ operator, onSessionExpired, onSignOut }: VerificationShellProps) {
  const [selectedMethod, setSelectedMethod] = useState<MethodId>("DIP_CHIP");
  const [sessionMethodFilter, setSessionMethodFilter] = useState<"ALL" | MethodId>("ALL");
  const [sessionStatusFilters, setSessionStatusFilters] = useState<string[]>([]);
  const [sessionLimit, setSessionLimit] = useState(20);
  const [createdByFilter, setCreatedByFilter] = useState("");
  const [createdFromFilter, setCreatedFromFilter] = useState("");
  const [createdToFilter, setCreatedToFilter] = useState("");
  const [identityNationalIdFilter, setIdentityNationalIdFilter] = useState("");
  const [methodCatalog, setMethodCatalog] = useState<VerificationMethodOption[]>(defaultMethods);
  const [sessions, setSessions] = useState<VerificationSession[]>([]);
  const [activeSession, setActiveSession] = useState<VerificationSession | null>(null);
  const [error, setError] = useState("");
  const [isStarting, setIsStarting] = useState(false);
  const [isLoadingSessions, setIsLoadingSessions] = useState(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [transactionLookup, setTransactionLookup] = useState("");
  const [isLookingUpTransaction, setIsLookingUpTransaction] = useState(false);
  const [isExportingSessions, setIsExportingSessions] = useState(false);
  const activeWorkflowIndex = workflowIndex(activeSession?.status);
  const operatorInitials = (operator.displayName || operator.username).slice(0, 2).toUpperCase();
  const navigationItems = [
    "Verification",
    "Transactions",
    hasPermission(operator, "METHOD_CATALOG_MANAGE") ? "Methods" : null,
    "Account",
    hasPermission(operator, "AUDIT_READ") ? "Audit" : null,
    hasPermission(operator, "OPERATOR_MANAGE") ? "Operators" : null
  ].filter((item): item is string => Boolean(item));
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
        const params = buildSessionSearchParams();
        const validationMessage = validateSessionSearchFilters();

        if (validationMessage) {
          setError(validationMessage);
          setSessions([]);
          setActiveSession(null);
          return;
        }

        const path = `/api/verification/sessions?${params.toString()}`;
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
  }, [operator.accessToken, sessionMethodFilter, sessionStatusFilters, sessionLimit, createdByFilter, createdFromFilter, createdToFilter, identityNationalIdFilter]);

  function handleApiError(err: unknown, fallback: string) {
    handleApiFailure(err, fallback, onSessionExpired, setError);
  }

  function buildSessionSearchParams() {
    const params = new URLSearchParams();

    if (sessionMethodFilter !== "ALL") {
      params.set("method", sessionMethodFilter);
    }

    if (sessionStatusFilters.length > 0) {
      params.set("status", sessionStatusFilters.join(","));
    }

    if (createdByFilter.trim()) {
      params.set("createdBy", createdByFilter.trim());
    }

    if (createdFromFilter) {
      params.set("createdFrom", toStartOfDayInstant(createdFromFilter));
    }

    if (createdToFilter) {
      params.set("createdTo", toEndOfDayInstant(createdToFilter));
    }

    if (identityNationalIdFilter.trim()) {
      params.set("identityNationalId", identityNationalIdFilter.trim());
    }

    params.set("limit", String(sessionLimit));
    return params;
  }

  function validateSessionSearchFilters() {
    if (createdByFilter.trim() && !uuidPattern.test(createdByFilter.trim())) {
      return "Created-by filter must be a valid operator UUID.";
    }

    if (createdFromFilter && createdToFilter && createdFromFilter > createdToFilter) {
      return "Created-from date must be before or equal to created-to date.";
    }

    if (identityNationalIdFilter.trim()) {
      return nationalIdValidationMessage(identityNationalIdFilter.trim());
    }

    return "";
  }

  function toggleSessionStatusFilter(status: string) {
    setSessionStatusFilters((current) => (
      current.includes(status)
        ? current.filter((item) => item !== status)
        : [...current, status]
    ));
  }

  function clearSessionFilters() {
    setSessionMethodFilter("ALL");
    setSessionStatusFilters([]);
    setSessionLimit(20);
    setCreatedByFilter("");
    setCreatedFromFilter("");
    setCreatedToFilter("");
    setIdentityNationalIdFilter("");
  }

  async function exportSessionsCsv() {
    const validationMessage = validateSessionSearchFilters();

    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setIsExportingSessions(true);
    setError("");

    try {
      const response = await fetch(`/api/verification/reports/sessions.csv?${buildSessionSearchParams().toString()}`, {
        headers: {
          Authorization: `Bearer ${operator.accessToken}`
        }
      });

      if (response.status === 401) {
        onSessionExpired();
        return;
      }

      if (!response.ok) {
        throw new Error(`Unable to export transactions. The server returned ${response.status}.`);
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "verification-sessions.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to export transactions.");
    } finally {
      setIsExportingSessions(false);
    }
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
        setSessionStatusFilters([]);
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
                <div className="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-3">
                  <div className="grid gap-3 sm:grid-cols-[180px_120px_minmax(160px,1fr)_auto] sm:items-end">
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
                      Limit
                      <select
                        className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                        value={sessionLimit}
                        onChange={(event) => setSessionLimit(Number(event.target.value))}
                      >
                        {sessionLimitOptions.map((limit) => (
                          <option key={limit} value={limit}>
                            {limit}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
                      Created By
                      <input
                        className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                        placeholder="Operator UUID"
                        value={createdByFilter}
                        onChange={(event) => setCreatedByFilter(event.target.value)}
                      />
                    </label>
                    <span className="rounded-md bg-white px-2.5 py-2 text-center text-xs font-bold text-slate-600 shadow-sm">{sessions.length}</span>
                  </div>

                  <div className="grid gap-3 sm:grid-cols-[160px_160px_minmax(180px,1fr)] sm:items-end">
                    <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
                      Created From
                      <input
                        className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                        type="date"
                        value={createdFromFilter}
                        onChange={(event) => setCreatedFromFilter(event.target.value)}
                      />
                    </label>
                    <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
                      Created To
                      <input
                        className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                        type="date"
                        value={createdToFilter}
                        onChange={(event) => setCreatedToFilter(event.target.value)}
                      />
                    </label>
                    <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
                      National ID
                      <input
                        className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
                        inputMode="numeric"
                        placeholder="13 digits"
                        value={identityNationalIdFilter}
                        onChange={(event) => setIdentityNationalIdFilter(event.target.value.replace(/\D/g, "").slice(0, 13))}
                      />
                    </label>
                  </div>

                  <fieldset className="grid gap-2">
                    <legend className="text-xs font-bold uppercase text-slate-500">Statuses</legend>
                    <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                      {statusFilters.map((status) => (
                        <label key={status} className="flex min-h-9 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-600 shadow-sm">
                          <input
                            className="size-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500"
                            type="checkbox"
                            checked={sessionStatusFilters.includes(status)}
                            onChange={() => toggleSessionStatusFilter(status)}
                          />
                          {status}
                        </label>
                      ))}
                    </div>
                  </fieldset>

                  <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
                    <button
                      className="min-h-10 rounded-lg border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 shadow-sm transition hover:bg-slate-100"
                      type="button"
                      onClick={clearSessionFilters}
                    >
                      Clear
                    </button>
                    <button
                      className="min-h-10 rounded-lg bg-teal-700 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-slate-300"
                      type="button"
                      onClick={() => void exportSessionsCsv()}
                      disabled={isExportingSessions || isLoadingSessions}
                    >
                      {isExportingSessions ? "Exporting..." : "Export CSV"}
                    </button>
                  </div>
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

          {hasPermission(operator, "METHOD_CATALOG_MANAGE") ? (
            <MethodCatalogPanel accessToken={operator.accessToken} onCatalogChanged={applyMethodCatalog} onError={setError} onSessionExpired={onSessionExpired} />
          ) : null}

          <AccountSecurityPanel
            accessToken={operator.accessToken}
            onError={setError}
            onSessionExpired={onSessionExpired}
          />

          {hasPermission(operator, "AUDIT_READ") ? (
            <AuditInquiryPanel accessToken={operator.accessToken} onError={setError} onSessionExpired={onSessionExpired} />
          ) : null}

          {hasPermission(operator, "OPERATOR_MANAGE") ? (
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

function toStartOfDayInstant(date: string) {
  return `${date}T00:00:00Z`;
}

function toEndOfDayInstant(date: string) {
  return `${date}T23:59:59Z`;
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
