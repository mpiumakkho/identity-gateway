import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { postJson } from "../../api/client";
import { isAuthenticationRequired } from "../../api/errors";
import { fieldLabelClassName, fieldTextAreaClassName } from "./formStyles";
import type { CloseVerificationPayload, VerificationCloseoutResult, VerificationSession } from "./types";

const finalStatuses = new Set(["APPROVED", "REJECTED"]);
const dopaStatuses = new Set(["DOPA_VERIFIED", "DOPA_REJECTED"]);

type SummaryPanelProps = {
  accessToken: string;
  session: VerificationSession | null;
  onError: (message: string) => void;
  onSaved: (session: VerificationSession) => void;
  onSessionExpired: () => void;
};

export function SummaryPanel({ accessToken, session, onError, onSaved, onSessionExpired }: SummaryPanelProps) {
  const [decision, setDecision] = useState<CloseVerificationPayload["decision"]>("APPROVED");
  const [notes, setNotes] = useState("");
  const [isClosing, setIsClosing] = useState(false);
  const [localMessage, setLocalMessage] = useState("");
  const [lastCloseout, setLastCloseout] = useState<VerificationCloseoutResult | null>(null);
  const isClosed = Boolean(session && finalStatuses.has(session.status));
  const canClose = Boolean(session && dopaStatuses.has(session.status));
  const forcedRejection = session?.status === "DOPA_REJECTED";

  useEffect(() => {
    setDecision("APPROVED");
    setNotes("");
    setLocalMessage("");
    setLastCloseout(null);
  }, [session?.transactionId]);

  useEffect(() => {
    if (forcedRejection) {
      setDecision("REJECTED");
    }
  }, [forcedRejection]);

  async function submitCloseout(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !canClose) {
      return;
    }

    const payload: CloseVerificationPayload = {
      decision,
      notes: notes.trim()
    };

    setIsClosing(true);
    setLocalMessage("");
    onError("");

    try {
      const response = await postJson<VerificationCloseoutResult>(
        `/api/verification/sessions/${session.transactionId}/closeout`,
        payload,
        { accessToken }
      );

      if (response.data) {
        setLastCloseout(response.data);
        setLocalMessage(`Transaction closed as ${response.data.decision}.`);
        onSaved({ ...session, status: response.data.sessionStatus });
      }
    } catch (err) {
      if (isAuthenticationRequired(err)) {
        onSessionExpired();
        return;
      }

      const message = err instanceof Error ? err.message : "Unable to close verification session.";
      setLocalMessage(message);
      onError(message);
    } finally {
      setIsClosing(false);
    }
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="summary-title">
      <div className="mb-5 flex items-start justify-between gap-3">
        <div className="flex gap-3">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-amber-50 text-sm font-black text-amber-700">05</span>
          <div>
            <h2 id="summary-title" className="text-xl font-bold text-slate-950">Verification Summary</h2>
            <p className="mt-1 text-sm text-slate-500">Close the transaction after DOPA validation and operator review.</p>
          </div>
        </div>
        {lastCloseout || isClosed ? <StatusBadge status={lastCloseout?.decision ?? session?.status ?? ""} /> : null}
      </div>

      {!session ? (
        <EmptyState message="Start a session before closeout." />
      ) : isClosed ? (
        <EmptyState message={`This transaction is already ${session.status.toLowerCase()}.`} />
      ) : !canClose ? (
        <EmptyState message="Complete DOPA validation before closeout." />
      ) : (
        <form className="grid gap-4" onSubmit={submitCloseout}>
          <div className="grid gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Verification decision">
            <DecisionButton
              decision="APPROVED"
              selected={decision === "APPROVED"}
              disabled={forcedRejection}
              onSelect={setDecision}
            />
            <DecisionButton decision="REJECTED" selected={decision === "REJECTED"} onSelect={setDecision} />
          </div>

          <label>
            <span className={fieldLabelClassName}>Notes</span>
            <textarea
              className={fieldTextAreaClassName}
              maxLength={1000}
              value={notes}
              onChange={(event) => setNotes(event.currentTarget.value)}
            />
          </label>

          {lastCloseout ? (
            <dl className="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
              <div>
                <dt className="text-xs font-bold uppercase text-slate-400">Decision</dt>
                <dd className={`mt-1 text-sm font-semibold ${decisionClassName(lastCloseout.decision)}`}>{lastCloseout.decision}</dd>
              </div>
              <div>
                <dt className="text-xs font-bold uppercase text-slate-400">Decided By</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">{lastCloseout.decidedBy.displayName}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-xs font-bold uppercase text-slate-400">Decided</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">{new Date(lastCloseout.decidedAt).toLocaleString()}</dd>
              </div>
            </dl>
          ) : null}

          {localMessage ? (
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-600" role="status">
              {localMessage}
            </div>
          ) : null}

          <button
            className="inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
            type="submit"
            disabled={isClosing}
          >
            {isClosing ? "Closing..." : "Close Transaction"}
          </button>
        </form>
      )}
    </section>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">
      {message}
    </div>
  );
}

function DecisionButton({
  decision,
  selected,
  disabled = false,
  onSelect
}: {
  decision: CloseVerificationPayload["decision"];
  selected: boolean;
  disabled?: boolean;
  onSelect: (decision: CloseVerificationPayload["decision"]) => void;
}) {
  return (
    <button
      className={
        selected
          ? "min-h-14 rounded-lg border border-slate-950 bg-slate-950 px-4 py-3 text-left text-sm font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-50"
          : "min-h-14 rounded-lg border border-slate-200 bg-white px-4 py-3 text-left text-sm font-bold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
      }
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={disabled}
      onClick={() => onSelect(decision)}
    >
      {decision === "APPROVED" ? "Approve" : "Reject"}
    </button>
  );
}

function StatusBadge({ status }: { status: string }) {
  return <span className={`rounded-md px-2.5 py-1 text-xs font-bold ${badgeClassName(status)}`}>{status === "APPROVED" ? "Approved" : "Rejected"}</span>;
}

function badgeClassName(status: string) {
  return status === "APPROVED" ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700";
}

function decisionClassName(status: string) {
  return status === "APPROVED" ? "text-emerald-700" : "text-red-700";
}
