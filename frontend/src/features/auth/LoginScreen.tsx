import { type FormEvent, useState } from "react";
import { postJson } from "../../api/client";
import type { AuthSession, LoginCredentials } from "./types";

type LoginScreenProps = {
  onAuthenticated: (session: AuthSession) => void;
};

const initialCredentials: LoginCredentials = {
  username: "",
  password: ""
};

export function LoginScreen({ onAuthenticated }: LoginScreenProps) {
  const [credentials, setCredentials] = useState<LoginCredentials>(initialCredentials);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      const response = await postJson<AuthSession>("/api/auth/login", credentials);

      if (!response.data) {
        throw new Error("Login did not return a session.");
      }

      onAuthenticated(response.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to sign in.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="grid min-h-screen bg-slate-950 text-white lg:grid-cols-[minmax(380px,0.9fr)_1.1fr]">
      <section className="flex min-h-screen items-center px-5 py-10 sm:px-8 lg:px-12">
        <div className="w-full max-w-md">
          <div className="mb-10 flex items-center gap-3">
            <div className="grid size-11 place-items-center rounded-lg bg-teal-400 text-sm font-black text-slate-950 shadow-sm shadow-teal-950/20">
              IG
            </div>
            <div>
              <p className="text-sm font-semibold text-white">Identity Gateway</p>
              <p className="text-xs text-slate-400">Operator Console</p>
            </div>
          </div>

          <div className="mb-8">
            <h1 className="text-3xl font-bold tracking-normal text-white">Sign in</h1>
            <p className="mt-2 text-sm leading-6 text-slate-400">Access verification operations with your operator account.</p>
          </div>

          <form className="grid gap-4" onSubmit={handleSubmit}>
            <label className="grid gap-2" htmlFor="username">
              <span className="text-sm font-semibold text-slate-200">Username</span>
              <input
                id="username"
                className="min-h-11 rounded-lg border border-white/10 bg-white px-3 py-2 text-sm font-medium text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-teal-300 focus:ring-2 focus:ring-teal-300/40"
                autoComplete="username"
                value={credentials.username}
                onChange={(event) => setCredentials((current) => ({ ...current, username: event.target.value }))}
                required
              />
            </label>

            <label className="grid gap-2" htmlFor="password">
              <span className="text-sm font-semibold text-slate-200">Password</span>
              <input
                id="password"
                className="min-h-11 rounded-lg border border-white/10 bg-white px-3 py-2 text-sm font-medium text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-teal-300 focus:ring-2 focus:ring-teal-300/40"
                autoComplete="current-password"
                type="password"
                value={credentials.password}
                onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))}
                required
              />
            </label>

            {error && (
              <div className="rounded-lg border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm font-medium text-red-100" role="alert">
                {error}
              </div>
            )}

            <button
              className="mt-2 inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-teal-400 px-4 py-2.5 text-sm font-bold text-slate-950 transition hover:bg-teal-300 disabled:cursor-not-allowed disabled:opacity-60"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Signing in..." : "Sign in"}
            </button>
          </form>
        </div>
      </section>

      <section className="hidden min-h-screen border-l border-white/10 bg-white text-slate-950 lg:block">
        <div className="flex h-full flex-col justify-between p-10">
          <div className="grid grid-cols-2 gap-3">
            {["Login", "Method", "Identity", "Summary"].map((step, index) => (
              <div key={step} className="rounded-lg border border-slate-200 bg-slate-50 p-4">
                <span className="grid size-8 place-items-center rounded-md bg-slate-950 text-xs font-bold text-white">{index + 1}</span>
                <strong className="mt-4 block text-sm font-bold text-slate-950">{step}</strong>
                <span className="mt-1 block text-sm text-slate-500">Controlled operator workflow</span>
              </div>
            ))}
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50 p-5">
            <p className="text-sm font-semibold text-slate-500">Security posture</p>
            <p className="mt-2 text-2xl font-bold tracking-normal text-slate-950">BCrypt password verification with server-side token tracking.</p>
          </div>
        </div>
      </section>
    </main>
  );
}