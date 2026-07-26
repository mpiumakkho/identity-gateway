import { useEffect, useState } from "react";
import { getJson, putJson } from "../../api/client";
import { handleApiFailure } from "../../api/errors";
import type { VerificationMethodOption } from "./types";

type MethodCatalogPanelProps = {
  accessToken: string;
  onCatalogChanged: (methods: VerificationMethodOption[]) => void;
  onError: (message: string) => void;
  onSessionExpired: () => void;
};

export function MethodCatalogPanel({ accessToken, onCatalogChanged, onError, onSessionExpired }: MethodCatalogPanelProps) {
  const [methods, setMethods] = useState<VerificationMethodOption[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [busyMethodId, setBusyMethodId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadCatalog() {
      setIsLoading(true);
      onError("");

      try {
        const response = await getJson<VerificationMethodOption[]>("/api/verification/methods/catalog", { accessToken });

        if (!cancelled) {
          setMethods(response.data ?? []);
          onCatalogChanged(response.data ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          handleApiError(err, "Unable to load verification method catalog.");
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadCatalog();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  function handleApiError(err: unknown, fallback: string) {
    handleApiFailure(err, fallback, onSessionExpired, onError);
  }

  async function updateMethod(method: VerificationMethodOption, enabled: boolean) {
    setBusyMethodId(method.id);
    onError("");

    try {
      const response = await putJson<VerificationMethodOption>(
        `/api/verification/methods/${method.id}/enabled`,
        { enabled },
        { accessToken }
      );
      const updatedMethod = response.data ?? { ...method, enabled };
      setMethods((current) => {
        const nextMethods = current.map((item) => (item.id === updatedMethod.id ? updatedMethod : item));
        onCatalogChanged(nextMethods);
        return nextMethods;
      });
    } catch (err) {
      handleApiError(err, "Unable to update verification method.");
    } finally {
      setBusyMethodId(null);
    }
  }

  return (
    <section className="mt-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" id="methods" aria-labelledby="methods-title">
      <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 id="methods-title" className="text-xl font-bold text-slate-950">Method Catalog</h2>
          <p className="mt-1 text-sm text-slate-500">Control which intake paths operators can start.</p>
        </div>
        <span className="self-start rounded-md bg-slate-100 px-2.5 py-2 text-xs font-bold text-slate-600 sm:self-auto">{methods.length}</span>
      </div>

      {isLoading ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">Loading methods...</div>
      ) : methods.length > 0 ? (
        <div className="overflow-hidden rounded-lg border border-slate-200">
          <div className="grid grid-cols-[1fr_120px_160px] bg-slate-50 px-4 py-2 text-xs font-bold uppercase text-slate-400 max-md:hidden">
            <span>Method</span>
            <span>Status</span>
            <span>Controls</span>
          </div>
          <div className="divide-y divide-slate-100">
            {methods.map((method) => {
              const isBusy = busyMethodId === method.id;

              return (
                <div key={method.id} className="grid gap-3 px-4 py-4 md:grid-cols-[1fr_120px_160px] md:items-center">
                  <div className="min-w-0">
                    <strong className="block truncate text-sm text-slate-950">{method.label}</strong>
                    <span className="mt-1 block text-xs text-slate-500">{method.description}</span>
                  </div>
                  <span className={method.enabled ? "text-sm font-semibold text-emerald-700" : "text-sm font-semibold text-red-700"}>
                    {method.enabled ? "Enabled" : "Disabled"}
                  </span>
                  <button
                    className={
                      method.enabled
                        ? "min-h-10 rounded-lg border border-red-200 bg-white px-3 text-sm font-bold text-red-700 shadow-sm transition hover:bg-red-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                        : "min-h-10 rounded-lg border border-emerald-200 bg-white px-3 text-sm font-bold text-emerald-700 shadow-sm transition hover:bg-emerald-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                    }
                    type="button"
                    onClick={() => void updateMethod(method, !method.enabled)}
                    disabled={isBusy}
                  >
                    {isBusy ? "Saving..." : method.enabled ? "Disable" : "Enable"}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">No methods configured.</div>
      )}
    </section>
  );
}
