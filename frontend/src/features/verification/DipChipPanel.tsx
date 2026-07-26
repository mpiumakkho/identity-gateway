import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { putJson } from "../../api/client";
import { isAuthenticationRequired } from "../../api/errors";
import { fieldInputClassName, fieldLabelClassName, fieldTextAreaClassName } from "./formStyles";
import { nationalIdValidationMessage } from "./nationalId";
import type { DipChipPayload, DipChipPayloadResult, VerificationSession } from "./types";

const emptyForm: DipChipPayload = {
  nationalId: "",
  title: "",
  firstName: "",
  lastName: "",
  dateOfBirth: "",
  laserCode: "",
  cardIssueDate: "",
  cardExpiryDate: "",
  readerName: "",
  readerSerialNumber: "",
  rawPayload: ""
};

const titles = ["Mr.", "Mrs.", "Ms.", "Miss"];

type DipChipPanelProps = {
  accessToken: string;
  session: VerificationSession | null;
  onError: (message: string) => void;
  onSaved: (session: VerificationSession) => void;
  onSessionExpired: () => void;
};

export function DipChipPanel({ accessToken, session, onError, onSaved, onSessionExpired }: DipChipPanelProps) {
  const [form, setForm] = useState<DipChipPayload>(emptyForm);
  const [isSaving, setIsSaving] = useState(false);
  const [localMessage, setLocalMessage] = useState("");
  const [lastSavedPayload, setLastSavedPayload] = useState<DipChipPayloadResult | null>(null);
  const canCapturePayload = session?.method === "DIP_CHIP";

  useEffect(() => {
    setForm(emptyForm);
    setLocalMessage("");
    setLastSavedPayload(null);
  }, [session?.transactionId]);

  function updateField(field: keyof DipChipPayload, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submitPayload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !canCapturePayload) {
      return;
    }

    const payload = trimPayload(form);
    const validationMessage = validatePayload(payload);

    if (validationMessage) {
      setLocalMessage(validationMessage);
      return;
    }

    setIsSaving(true);
    setLocalMessage("");
    onError("");

    try {
      const response = await putJson<DipChipPayloadResult>(
        `/api/verification/sessions/${session.transactionId}/dip-chip-payload`,
        payload,
        { accessToken }
      );

      if (response.data) {
        setLastSavedPayload(response.data);
        setLocalMessage(`Card payload captured for ${response.data.maskedNationalId}.`);
        onSaved({ ...session, status: response.data.sessionStatus });
      }
    } catch (err) {
      if (isAuthenticationRequired(err)) {
        onSessionExpired();
        return;
      }

      const message = err instanceof Error ? err.message : "Unable to save Dip Chip payload.";
      setLocalMessage(message);
      onError(message);
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="dip-chip-title">
      <div className="mb-5 flex items-start justify-between gap-3">
        <div className="flex gap-3">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-cyan-50 text-sm font-black text-cyan-700">03</span>
          <div>
            <h2 id="dip-chip-title" className="text-xl font-bold text-slate-950">Dip Chip Payload</h2>
            <p className="mt-1 text-sm text-slate-500">Capture normalized reader data for card-based transactions.</p>
          </div>
        </div>
        {lastSavedPayload ? <span className="rounded-md bg-cyan-50 px-2.5 py-1 text-xs font-bold text-cyan-700">Saved</span> : null}
      </div>

      {!session ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">
          Start a Dip Chip session before submitting reader payload.
        </div>
      ) : !canCapturePayload ? (
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-500">
          Dip Chip payload capture is available for Dip Chip sessions only.
        </div>
      ) : (
        <form className="grid gap-4" onSubmit={submitPayload}>
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

          <div className="grid gap-4 sm:grid-cols-2">
            <label>
              <span className={fieldLabelClassName}>Card Issue Date</span>
              <input
                className={fieldInputClassName}
                type="date"
                value={form.cardIssueDate}
                onChange={(event) => updateField("cardIssueDate", event.currentTarget.value)}
                required
              />
            </label>
            <label>
              <span className={fieldLabelClassName}>Card Expiry Date</span>
              <input
                className={fieldInputClassName}
                type="date"
                value={form.cardExpiryDate}
                onChange={(event) => updateField("cardExpiryDate", event.currentTarget.value)}
                required
              />
            </label>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label>
              <span className={fieldLabelClassName}>Reader Name</span>
              <input
                className={fieldInputClassName}
                maxLength={80}
                value={form.readerName}
                onChange={(event) => updateField("readerName", event.currentTarget.value)}
                required
              />
            </label>
            <label>
              <span className={fieldLabelClassName}>Reader Serial</span>
              <input
                className={fieldInputClassName}
                maxLength={80}
                value={form.readerSerialNumber}
                onChange={(event) => updateField("readerSerialNumber", event.currentTarget.value)}
                required
              />
            </label>
          </div>

          <label>
            <span className={fieldLabelClassName}>Raw Payload</span>
            <textarea
              className={fieldTextAreaClassName}
              maxLength={10000}
              value={form.rawPayload}
              onChange={(event) => updateField("rawPayload", event.currentTarget.value)}
              required
            />
          </label>

          {localMessage ? (
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-600" role="status">
              {localMessage}
            </div>
          ) : null}

          <button
            className="inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-cyan-700 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-cyan-600 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
            type="submit"
            disabled={isSaving}
          >
            {isSaving ? "Saving..." : session.status === "IDENTITY_CAPTURED" ? "Update Payload" : "Save Payload"}
          </button>
        </form>
      )}
    </section>
  );
}

function trimPayload(payload: DipChipPayload): DipChipPayload {
  return {
    nationalId: payload.nationalId.trim(),
    title: payload.title.trim(),
    firstName: payload.firstName.trim(),
    lastName: payload.lastName.trim(),
    dateOfBirth: payload.dateOfBirth,
    laserCode: payload.laserCode.trim(),
    cardIssueDate: payload.cardIssueDate,
    cardExpiryDate: payload.cardExpiryDate,
    readerName: payload.readerName.trim(),
    readerSerialNumber: payload.readerSerialNumber.trim(),
    rawPayload: payload.rawPayload.trim()
  };
}

function validatePayload(payload: DipChipPayload) {
  const nationalIdMessage = nationalIdValidationMessage(payload.nationalId);
  if (nationalIdMessage) {
    return nationalIdMessage;
  }

  if (payload.cardIssueDate && payload.cardExpiryDate && payload.cardExpiryDate < payload.cardIssueDate) {
    return "Card expiry date must be on or after the issue date.";
  }

  return "";
}
