import { useEffect, useState } from "react";
import { ApiError, getJson } from "../../api/client";

type SystemHealthPanelProps = {
  accessToken: string;
  onError: (message: string) => void;
  onSessionExpired: () => void;
};

type SystemHealth = {
  service: string;
  status: string;
  databaseStatus: string;
  checkedAt: string;
};

export function SystemHealthPanel({ accessToken, onError, onSessionExpired }: SystemHealthPanelProps) {
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  async function loadHealth() {
    setIsLoading(true);
    onError("");

    try {
      const response = await getJson<SystemHealth>("/api/system/health", { accessToken });
      setHealth(response.data);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onSessionExpired();
        return;
      }

      onError(err instanceof Error ? err.message : "Unable to load system health.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    let cancelled = false;

    async function loadInitialHealth() {
      setIsLoading(true);
      onError("");

      try {
        const response = await getJson<SystemHealth>("/api/system/health", { accessToken });

        if (!cancelled) {
          setHealth(response.data);
        }
      } catch (err) {
        if (cancelled) {
          return;
        }

        if (err instanceof ApiError && err.status === 401) {
          onSessionExpired();
          return;
        }

        onError(err instanceof Error ? err.message : "Unable to load system health.");
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadInitialHealth();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  return (
    <section className="mb-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="system-health-title">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 id="system-health-title" className="text-xl font-bold text-slate-950">System Health</h2>
          <p className="mt-1 text-sm text-slate-500">Service and database readiness for current operations.</p>
        </div>
        <button
          className="min-h-10 rounded-lg border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          type="button"
          onClick={() => void loadHealth()}
          disabled={isLoading}
        >
          {isLoading ? "Checking..." : "Refresh"}
        </button>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        <HealthItem label="Service" value={health?.service ?? "identity-gateway"} />
        <HealthItem label="API" value={health?.status ?? "UNKNOWN"} status={health?.status} />
        <HealthItem label="Database" value={health?.databaseStatus ?? "UNKNOWN"} status={health?.databaseStatus} />
      </div>

      <p className="mt-3 text-xs font-medium text-slate-400">Last checked: {health?.checkedAt ? formatDateTime(health.checkedAt) : "Not checked"}</p>
    </section>
  );
}

function HealthItem({ label, value, status }: { label: string; value: string; status?: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
      <span className="text-xs font-bold uppercase text-slate-400">{label}</span>
      <strong className={`mt-2 block text-lg font-bold ${statusClassName(status)}`}>{value}</strong>
    </div>
  );
}

function statusClassName(status?: string) {
  if (status === "UP") {
    return "text-emerald-700";
  }

  if (status === "DOWN") {
    return "text-red-700";
  }

  return "text-slate-700";
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString();
}
