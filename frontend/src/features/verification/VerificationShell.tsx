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
    <main className="app-shell">
      <aside className="sidebar" aria-label="Primary navigation">
        <div className="brand-mark">IG</div>
        <nav>
          <a className="nav-item active" href="#verification">Verification</a>
          <a className="nav-item" href="#transactions">Transactions</a>
          <a className="nav-item" href="#audit">Audit</a>
        </nav>
      </aside>

      <section className="workspace" id="verification">
        <header className="topbar">
          <div>
            <h1>Identity Gateway</h1>
            <p>Verification operations</p>
          </div>
          <div className="operator-chip">Operator Console</div>
        </header>

        <div className="workflow-grid">
          <section className="panel method-panel" aria-labelledby="method-title">
            <div className="panel-heading">
              <span className="step-index">01</span>
              <div>
                <h2 id="method-title">Verification Method</h2>
                <p>Select the intake path for this transaction.</p>
              </div>
            </div>

            <div className="method-list">
              {methods.map((method) => (
                <button
                  key={method.id}
                  className={method.id === selectedMethod ? "method-option selected" : "method-option"}
                  type="button"
                  onClick={() => setSelectedMethod(method.id)}
                >
                  <span>{method.label}</span>
                  <small>{method.detail}</small>
                </button>
              ))}
            </div>

            <button className="primary-action" type="button" onClick={startSession} disabled={isStarting}>
              {isStarting ? "Starting..." : "Start Session"}
            </button>
          </section>

          <section className="panel status-panel" aria-labelledby="status-title">
            <div className="panel-heading">
              <span className="step-index">02</span>
              <div>
                <h2 id="status-title">Session Status</h2>
                <p>Current transaction state.</p>
              </div>
            </div>

            {error && <div className="alert error">{error}</div>}

            {session ? (
              <dl className="session-detail">
                <div>
                  <dt>Transaction ID</dt>
                  <dd>{session.transactionId}</dd>
                </div>
                <div>
                  <dt>Method</dt>
                  <dd>{session.method}</dd>
                </div>
                <div>
                  <dt>Status</dt>
                  <dd>{session.status}</dd>
                </div>
                <div>
                  <dt>Created</dt>
                  <dd>{new Date(session.createdAt).toLocaleString()}</dd>
                </div>
              </dl>
            ) : (
              <div className="empty-state">
                <strong>No active session</strong>
                <span>Choose a method to create the first transaction.</span>
              </div>
            )}
          </section>
        </div>
      </section>
    </main>
  );
}