import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApiError, getJson, postJson } from "../../api/client";
import { fieldInputClassName, fieldLabelClassName } from "./formStyles";
import type { DopaValidationHistory, DopaValidationPayload, DopaValidationResult, VerificationSession } from "./types";

const emptyForm: DopaValidationPayload = {
  consentReference: ""
};

const allowedStatuses = new Set(["IDENTITY_CAPTURED", "DOPA_VERIFIED", "DOPA_REJECTED"]);

type DopaPanelProps = {
  accessToken: string;
  session: VerificationSession | null;
  onError: (message: string) => void;
  onSaved: (session: VerificationSession) => void;
  onSessionExpired: () => void;
};

export function DopaPanel({ accessToken, session, onError, onSaved, onSessionExpired }: DopaPanelProps) {
  const [form, setForm] = useState<DopaValidationPayload>(emptyForm);
  const [history, setHistory] = useState<DopaValidationHistory[]>([]);
  const [isValidating, setIsValidating] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [localMessage, setLocalMessage] = useState("");
  const [lastResult, setLastResult] = useState<DopaValidationResult | null>(null);
  const canValidate = Boolean(session && allowedStatuses.has(session.status));

  useEffect(() => {
    let cancelled = false;

    async function loadHistory(transactionId: string) {
      setIsLoadingHistory(true);
      setHistory([]);

      try {
        const response = await getJson<DopaValidationHistory[]>(`/api/verification/sessions/${transactionId}/dopa-validations`, {
          accessToken
        });

        if (!cancelled) {
          setHistory(response.data ?? []);
        }
      } catch (err) {
        if (cancelled) {
          return;
        }

        if (err instanceof ApiError && err.status === 401) {
          onSessionExpired();
          return;
        }

        const message = err instanceof Error ? err.message : "Unable to load DOPA validation history.";
        setLocalMessage(message);
        onError(message);
      } finally {
        if (!cancelled) {
          setIsLoadingHistory(false);
        }
      }
    }

    setForm(emptyForm);
    setLocalMessage("");
    setLastResult(null);

    if (session) {
      void loadHistory(session.transactionId);
    } else {
      setHistory([]);
    }

    return () => {
      cancelled = true;
    };
  }, [accessToken, session?.transactionId, session?.status]);

  async function refreshHistory(transactionId: string) {
    const response = await getJson<DopaValidationHistory[]>(`/api/verification/sessions/${transactionId}/dopa-validations`, {
      accessToken
    });
    setHistory(response.data ?? []);
  }

  async function submitValidation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !canValidate) {
      return;
    }

    const payload = { consentReference: form.consentReference.trim() };

    if (!payload.consentReference) {
      setLocalMessage("Consent reference is required.");
      return;
    }

    setIsValidating(true);
    setLocalMessage("");
    onError("");

    try {
      const response = await postJson<DopaValidationResult>(
        `/api/verification/sessions/${session.transactionId}/dopa-validation`,
        payload,
        { accessToken }
      );

      if (response.data) {
        setLastResult(response.data);
        setLocalMessage(response.data.responseMessage);
        onSaved({ ...session, status: response.data.sessionStatus });
        await refreshHistory(session.transactionId);
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onSessionExpired();
        return;
      }

      const message = err instanceof Error ? err.message : "Unable to validate identity with DOPA.";
      setLocalMessage(message);
      onError(message);
    } finally {
      setIsValidating(false);
    }
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="dopa-title">
      <div className="mb-5 flex items-start justify-between gap-3">
        <div className="flex gap-3">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-emerald-50 text-sm font-black text-emerald-700">04</span>
          <div>
            <h2 id="dopa-title" className="text-xl font-bold text-slate-950">DOPA Validation</h2>
            <p className="mt-1 text-sm text-slate-500">Validate captured identity details against the configured citizen registry connector.</p>
          </div>
        </div>
        {lastResult ? <ResultBadge status={lastResult.validationStatus} /> : null}
      </div>

      {!session ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">
          Start a session and capture identity details before validation.
        </div>
      ) : !canValidate ? (
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-500">
          Capture identity details before running DOPA validation.
        </div>
      ) : (
        <form className="grid gap-4" onSubmit={submitValidation}>
          <label>
            <span className={fieldLabelClassName}>Consent Reference</span>
            <input
              className={fieldInputClassName}
              maxLength={80}
              value={form.consentReference}
              onChange={(event) => setForm({ consentReference: event.currentTarget.value })}
              required
            />
          </label>

          {lastResult ? (
            <dl className="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
              <div>
                <dt className="text-xs font-bold uppercase text-slate-400">Validation</dt>
                <dd className={`mt-1 text-sm font-semibold ${resultClassName(lastResult.validationStatus)}`}>{lastResult.validationStatus}</dd>
              </div>
              <div>
                <dt className="text-xs font-bold uppercase text-slate-400">Identity Source</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">{lastResult.identitySource}</dd>
              </div>
              <div>
                <dt className="text-xs font-bold uppercase text-slate-400">National ID</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">{lastResult.maskedNationalId}</dd>
              </div>
              <div>
                <dt className="text-xs font-bold uppercase text-slate-400">Response Code</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">{lastResult.responseCode}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-xs font-bold uppercase text-slate-400">Validated</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">{new Date(lastResult.validatedAt).toLocaleString()}</dd>
              </div>
            </dl>
          ) : null}

          {localMessage ? (
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-600" role="status">
              {localMessage}
            </div>
          ) : null}

          <button
            className="inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-emerald-700 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
            type="submit"
            disabled={isValidating}
          >
            {isValidating ? "Validating..." : "Run DOPA Validation"}
          </button>
        </form>
      )}

      {session ? (
        <div className="mt-5 border-t border-slate-100 pt-5">
          <div className="mb-3 flex items-center justify-between gap-3">
            <h3 className="text-base font-bold text-slate-950">Validation History</h3>
            {isLoadingHistory ? <span className="text-xs font-semibold text-slate-400">Loading</span> : null}
          </div>
          {history.length > 0 ? (
            <ol className="grid gap-3">
              {history.map((attempt) => (
                <li key={attempt.attemptId} className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
                  <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                    <span className={`text-sm font-bold ${resultClassName(attempt.validationStatus)}`}>{attempt.validationStatus}</span>
                    <time className="text-xs font-semibold text-slate-400" dateTime={attempt.validatedAt}>
                      {new Date(attempt.validatedAt).toLocaleString()}
                    </time>
                  </div>
                  <div className="mt-2 grid gap-2 text-xs font-semibold text-slate-500 sm:grid-cols-3">
                    <span>{attempt.identitySource}</span>
                    <span>{attempt.responseCode}</span>
                    <span>{attempt.consentReference}</span>
                  </div>
                  <p className="mt-2 text-sm text-slate-600">{attempt.responseMessage}</p>
                </li>
              ))}
            </ol>
          ) : (
            <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-6 text-center text-sm font-medium text-slate-500">
              No DOPA validation attempts yet.
            </div>
          )}
        </div>
      ) : null}
    </section>
  );
}

function ResultBadge({ status }: { status: string }) {
  const matched = status === "MATCHED";

  return (
    <span className={matched ? "rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700" : "rounded-md bg-red-50 px-2.5 py-1 text-xs font-bold text-red-700"}>
      {matched ? "Matched" : "Not matched"}
    </span>
  );
}

function resultClassName(status: string) {
  return status === "MATCHED" ? "text-emerald-700" : "text-red-700";
}
