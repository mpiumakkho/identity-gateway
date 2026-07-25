import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApiError, putJson } from "../../api/client";
import { fieldInputClassName, fieldLabelClassName } from "./formStyles";
import { nationalIdValidationMessage } from "./nationalId";
import type { ManualIdentityPayload, ManualIdentityResult, VerificationSession } from "./types";

const emptyForm: ManualIdentityPayload = {
  nationalId: "",
  title: "",
  firstName: "",
  lastName: "",
  dateOfBirth: "",
  laserCode: ""
};

const titles = ["Mr.", "Mrs.", "Ms.", "Miss"];

type ManualIdentityPanelProps = {
  accessToken: string;
  session: VerificationSession | null;
  onError: (message: string) => void;
  onSaved: (session: VerificationSession) => void;
  onSessionExpired: () => void;
};

export function ManualIdentityPanel({ accessToken, session, onError, onSaved, onSessionExpired }: ManualIdentityPanelProps) {
  const [form, setForm] = useState<ManualIdentityPayload>(emptyForm);
  const [isSaving, setIsSaving] = useState(false);
  const [localMessage, setLocalMessage] = useState("");
  const [lastSavedIdentity, setLastSavedIdentity] = useState<ManualIdentityResult | null>(null);
  const canCaptureIdentity = session?.method === "MANUAL_ENTRY";

  useEffect(() => {
    setForm(emptyForm);
    setLocalMessage("");
    setLastSavedIdentity(null);
  }, [session?.transactionId]);

  function updateField(field: keyof ManualIdentityPayload, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submitIdentity(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !canCaptureIdentity) {
      return;
    }

    const payload = trimPayload(form);

    const nationalIdMessage = nationalIdValidationMessage(payload.nationalId);
    if (nationalIdMessage) {
      setLocalMessage(nationalIdMessage);
      return;
    }

    setIsSaving(true);
    setLocalMessage("");
    onError("");

    try {
      const response = await putJson<ManualIdentityResult>(
        `/api/verification/sessions/${session.transactionId}/manual-identity`,
        payload,
        { accessToken }
      );

      if (response.data) {
        setLastSavedIdentity(response.data);
        setLocalMessage(`Identity captured for ${response.data.maskedNationalId}.`);
        onSaved({ ...session, status: response.data.sessionStatus });
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onSessionExpired();
        return;
      }

      const message = err instanceof Error ? err.message : "Unable to save manual identity.";
      setLocalMessage(message);
      onError(message);
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="manual-identity-title">
      <div className="mb-5 flex items-start justify-between gap-3">
        <div className="flex gap-3">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-indigo-50 text-sm font-black text-indigo-700">03</span>
          <div>
            <h2 id="manual-identity-title" className="text-xl font-bold text-slate-950">Manual Identity</h2>
            <p className="mt-1 text-sm text-slate-500">Capture citizen-card details for manual-entry transactions.</p>
          </div>
        </div>
        {lastSavedIdentity ? <span className="rounded-md bg-indigo-50 px-2.5 py-1 text-xs font-bold text-indigo-700">Saved</span> : null}
      </div>

      {!session ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">
          Start a manual-entry session before capturing identity details.
        </div>
      ) : !canCaptureIdentity ? (
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-500">
          Manual identity capture is available for Manual Entry sessions only.
        </div>
      ) : (
        <form className="grid gap-4" onSubmit={submitIdentity}>
          <div className="grid gap-4 sm:grid-cols-[120px_1fr]">
            <label>
              <span className={fieldLabelClassName}>Title</span>
              <select
                className={fieldInputClassName}
                value={form.title}
                onChange={(event) => updateField("title", event.currentTarget.value)}
                required
              >
                <option value="">Select</option>
                {titles.map((title) => (
                  <option key={title} value={title}>
                    {title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span className={fieldLabelClassName}>National ID</span>
              <input
                className={fieldInputClassName}
                inputMode="numeric"
                maxLength={13}
                pattern="[0-9]{13}"
                placeholder="13 digits"
                value={form.nationalId}
                onChange={(event) => updateField("nationalId", event.currentTarget.value.replace(/\D/g, "").slice(0, 13))}
                required
              />
            </label>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label>
              <span className={fieldLabelClassName}>First Name</span>
              <input
                className={fieldInputClassName}
                maxLength={80}
                value={form.firstName}
                onChange={(event) => updateField("firstName", event.currentTarget.value)}
                required
              />
            </label>
            <label>
              <span className={fieldLabelClassName}>Last Name</span>
              <input
                className={fieldInputClassName}
                maxLength={80}
                value={form.lastName}
                onChange={(event) => updateField("lastName", event.currentTarget.value)}
                required
              />
            </label>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label>
              <span className={fieldLabelClassName}>Date of Birth</span>
              <input
                className={fieldInputClassName}
                type="date"
                value={form.dateOfBirth}
                onChange={(event) => updateField("dateOfBirth", event.currentTarget.value)}
                required
              />
            </label>
            <label>
              <span className={fieldLabelClassName}>Laser Code</span>
              <input
                className={fieldInputClassName}
                maxLength={20}
                minLength={8}
                value={form.laserCode}
                onChange={(event) => updateField("laserCode", event.currentTarget.value.toUpperCase())}
                required
              />
            </label>
          </div>

          {localMessage ? (
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-600" role="status">
              {localMessage}
            </div>
          ) : null}

          <button
            className="inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-indigo-700 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-indigo-600 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
            type="submit"
            disabled={isSaving}
          >
            {isSaving ? "Saving..." : session.status === "IDENTITY_CAPTURED" ? "Update Identity" : "Save Identity"}
          </button>
        </form>
      )}
    </section>
  );
}

function trimPayload(payload: ManualIdentityPayload): ManualIdentityPayload {
  return {
    nationalId: payload.nationalId.trim(),
    title: payload.title.trim(),
    firstName: payload.firstName.trim(),
    lastName: payload.lastName.trim(),
    dateOfBirth: payload.dateOfBirth,
    laserCode: payload.laserCode.trim()
  };
}
