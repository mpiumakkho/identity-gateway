import { useState, type FormEvent } from "react";
import { ApiError, putJson } from "../../api/client";

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

const emptyPasswordForm: PasswordForm = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: ""
};

export function AccountSecurityPanel({ accessToken, onError, onSessionExpired }: AccountSecurityPanelProps) {
  const [form, setForm] = useState<PasswordForm>(emptyPasswordForm);
  const [successMessage, setSuccessMessage] = useState("");
  const [isSaving, setIsSaving] = useState(false);

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
    } catch (err) {
      if (err instanceof ApiError && err.status === 401 && err.code === "AUTHENTICATION_REQUIRED") {
        onSessionExpired();
        return;
      }

      onError(err instanceof Error ? err.message : "Unable to change password.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="mt-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" id="account" aria-labelledby="account-title">
      <div className="mb-5">
        <h2 id="account-title" className="text-xl font-bold text-slate-950">Account Security</h2>
        <p className="mt-1 text-sm text-slate-500">Change your operator password.</p>
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
    </section>
  );
}
