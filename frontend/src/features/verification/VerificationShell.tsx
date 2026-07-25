import { useState } from "react";
import { postJson } from "../../api/client";

type MethodId = "DIP_CHIP" | "MANUAL_ENTRY";

type VerificationSession = {
  transactionId: string;
  method: MethodId;
  status: string;
  createdAt: string;
};

const methods: Array<{ id: MethodId; label: string; detail: string }> = [
  { id: "DIP_CHIP", label: "Dip Chip", detail: "Card reader" },
  { id: "MANUAL_ENTRY", label: "Manual Entry", detail: "Controlled form" }
];

const workflowSteps = ["Method", "Identity", "DOPA", "Summary"];

export function VerificationShell() {
  const [selectedMethod, setSelectedMethod] = useState<MethodId>("DIP_CHIP");
  const [session, setSession] = useState<VerificationSession | null>(null);
  const [error, setError] = useState("");
  const [isStarting, setIsStarting] = useState(false);

  async function startSession() {
    setIsStarting(true);
    setError("");

    try {
      const response = await postJson<VerificationSession>("/api/verification/sessions", {
        method: selectedMethod
      });
      setSession(response.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to start verification session.");
    } finally {
      setIsStarting(false);
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
            {[
              ["Verification", "active"],
              ["Transactions", ""],
              ["Audit", ""]
            ].map(([label, state]) => (
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
            ))}
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
                <span className="grid size-7 place-items-center rounded-md bg-teal-100 text-xs font-black text-teal-800">OP</span>
                Operator
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
                <button className="flex w-full items-center rounded-md px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100" type="button">
                  Sign out
                </button>
              </div>
            </div>
          </header>

          <div className="mb-5 grid grid-cols-2 gap-2 rounded-lg border border-slate-200 bg-white p-2 shadow-sm sm:grid-cols-4">
            {workflowSteps.map((step, index) => (
              <div key={step} className="flex items-center gap-2 rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-600">
                <span className={index === 0 ? "grid size-6 place-items-center rounded-md bg-teal-600 text-xs font-bold text-white" : "grid size-6 place-items-center rounded-md bg-slate-200 text-xs font-bold text-slate-600"}>
                  {index + 1}
                </span>
                <span className="font-medium">{step}</span>
              </div>
            ))}
          </div>

          <div className="grid gap-5 xl:grid-cols-[minmax(340px,520px)_1fr]">
            <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="method-title">
              <div className="mb-5 flex gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-teal-50 text-sm font-black text-teal-700">01</span>
                <div>
                  <h2 id="method-title" className="text-xl font-bold text-slate-950">Verification Method</h2>
                  <p className="mt-1 text-sm text-slate-500">Select the intake path for this transaction.</p>
                </div>
              </div>

              <div className="grid gap-3">
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
                        <span className="text-sm text-slate-500">{method.detail}</span>
                      </span>
                      <span className={selected ? "grid size-5 place-items-center rounded-full bg-teal-600" : "size-5 rounded-full border border-slate-300"}>
                        {selected && <span className="size-2 rounded-full bg-white" />}
                      </span>
                    </button>
                  );
                })}
              </div>

              <button
                className="mt-5 inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                type="button"
                onClick={startSession}
                disabled={isStarting}
              >
                {isStarting ? "Starting..." : "Start Session"}
              </button>
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="status-title">
              <div className="mb-5 flex gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-slate-100 text-sm font-black text-slate-700">02</span>
                <div>
                  <h2 id="status-title" className="text-xl font-bold text-slate-950">Session Status</h2>
                  <p className="mt-1 text-sm text-slate-500">Current transaction state.</p>
                </div>
              </div>

              {error && (
                <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
                  {error}
                </div>
              )}

              {session ? (
                <dl className="grid gap-4">
                  <div className="border-b border-slate-100 pb-4">
                    <dt className="text-xs font-bold uppercase text-slate-400">Transaction ID</dt>
                    <dd className="mt-1 break-all text-sm font-semibold text-slate-950">{session.transactionId}</dd>
                  </div>
                  <div className="grid gap-4 border-b border-slate-100 pb-4 sm:grid-cols-3">
                    <div>
                      <dt className="text-xs font-bold uppercase text-slate-400">Method</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-950">{session.method}</dd>
                    </div>
                    <div>
                      <dt className="text-xs font-bold uppercase text-slate-400">Status</dt>
                      <dd className="mt-1 text-sm font-semibold text-teal-700">{session.status}</dd>
                    </div>
                    <div>
                      <dt className="text-xs font-bold uppercase text-slate-400">Created</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-950">{new Date(session.createdAt).toLocaleString()}</dd>
                    </div>
                  </div>
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
          </div>
        </section>
      </div>
    </main>
  );
}