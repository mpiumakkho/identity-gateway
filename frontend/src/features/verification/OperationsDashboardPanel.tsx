import { useEffect, useState } from "react";
import { ApiError, getJson } from "../../api/client";

type OperationsDashboardPanelProps = {
  accessToken: string;
  onError: (message: string) => void;
  onSessionExpired: () => void;
};

type MetricCount = {
  key: string;
  count: number;
};

type DashboardMetrics = {
  totalTransactions: number;
  byStatus: MetricCount[];
  byMethod: MetricCount[];
};

const importantStatuses = ["CREATED", "IDENTITY_CAPTURED", "DOPA_VERIFIED", "DOPA_REJECTED", "APPROVED", "REJECTED"];

export function OperationsDashboardPanel({ accessToken, onError, onSessionExpired }: OperationsDashboardPanelProps) {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadMetrics() {
      setIsLoading(true);
      onError("");

      try {
        const response = await getJson<DashboardMetrics>("/api/verification/dashboard", { accessToken });

        if (!cancelled) {
          setMetrics(response.data);
        }
      } catch (err) {
        if (cancelled) {
          return;
        }

        if (err instanceof ApiError && err.status === 401) {
          onSessionExpired();
          return;
        }

        onError(err instanceof Error ? err.message : "Unable to load dashboard metrics.");
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadMetrics();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const statusCounts = new Map((metrics?.byStatus ?? []).map((item) => [item.key, item.count]));
  const methodCounts = new Map((metrics?.byMethod ?? []).map((item) => [item.key, item.count]));

  return (
    <section className="mb-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="dashboard-title">
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 id="dashboard-title" className="text-xl font-bold text-slate-950">Operations Dashboard</h2>
          <p className="mt-1 text-sm text-slate-500">Current transaction volume by status and intake method.</p>
        </div>
        {isLoading ? <span className="text-xs font-semibold text-slate-400">Loading</span> : null}
      </div>

      <div className="grid gap-3 lg:grid-cols-[220px_1fr]">
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
          <span className="text-xs font-bold uppercase text-slate-400">Total Transactions</span>
          <strong className="mt-3 block text-3xl font-bold tracking-normal text-slate-950">{metrics?.totalTransactions ?? 0}</strong>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {importantStatuses.map((status) => (
            <div key={status} className="rounded-lg border border-slate-200 bg-white p-4">
              <span className="text-xs font-bold uppercase text-slate-400">{statusLabel(status)}</span>
              <strong className={`mt-2 block text-2xl font-bold tracking-normal ${statusClassName(status)}`}>{statusCounts.get(status) ?? 0}</strong>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <MethodMetric label="Dip Chip" count={methodCounts.get("DIP_CHIP") ?? 0} />
        <MethodMetric label="Manual Entry" count={methodCounts.get("MANUAL_ENTRY") ?? 0} />
      </div>
    </section>
  );
}

function MethodMetric({ label, count }: { label: string; count: number }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
      <span className="text-sm font-semibold text-slate-600">{label}</span>
      <strong className="text-lg font-bold text-slate-950">{count}</strong>
    </div>
  );
}

function statusLabel(status: string) {
  return status
    .split("_")
    .map((part) => part.slice(0, 1) + part.slice(1).toLowerCase())
    .join(" ");
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
