import { useEffect, useState } from "react";
import { ApiError, getJson } from "../../api/client";
import type { AuditEvent } from "../verification/types";

type AuditInquiryPanelProps = {
  accessToken: string;
  onError: (message: string) => void;
  onSessionExpired: () => void;
};

const eventTypes = [
  "ALL",
  "AUTH_LOGIN_SUCCEEDED",
  "AUTH_LOGIN_FAILED",
  "AUTH_LOGOUT",
  "AUTH_PASSWORD_CHANGED",
  "AUTH_SESSION_REVOKED",
  "OPERATOR_CREATED",
  "OPERATOR_PASSWORD_CHANGED",
  "OPERATOR_DISABLED",
  "VERIFICATION_SESSION_CREATED",
  "MANUAL_IDENTITY_CAPTURED",
  "DIP_CHIP_PAYLOAD_CAPTURED",
  "DOPA_VALIDATION_COMPLETED",
  "VERIFICATION_CLOSED"
];

export function AuditInquiryPanel({ accessToken, onError, onSessionExpired }: AuditInquiryPanelProps) {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [eventType, setEventType] = useState("ALL");
  const [limit, setLimit] = useState(50);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadEvents() {
      setIsLoading(true);
      onError("");

      try {
        const params = new URLSearchParams();
        params.set("limit", String(limit));

        if (eventType !== "ALL") {
          params.set("eventType", eventType);
        }

        const response = await getJson<AuditEvent[]>(`/api/audit-events?${params.toString()}`, { accessToken });

        if (!cancelled) {
          setEvents(response.data ?? []);
        }
      } catch (err) {
        if (cancelled) {
          return;
        }

        if (err instanceof ApiError && err.status === 401) {
          onSessionExpired();
          return;
        }

        onError(err instanceof Error ? err.message : "Unable to load audit events.");
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadEvents();

    return () => {
      cancelled = true;
    };
  }, [accessToken, eventType, limit]);

  return (
    <section className="mt-5 rounded-lg border border-slate-200 bg-white p-5 shadow-sm" id="audit" aria-labelledby="audit-inquiry-title">
      <div className="mb-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 id="audit-inquiry-title" className="text-xl font-bold text-slate-950">Audit Inquiry</h2>
          <p className="mt-1 text-sm text-slate-500">Review platform activity across authentication, operators, and transactions.</p>
        </div>
        <div className="grid gap-3 sm:grid-cols-[260px_120px_auto] sm:items-end">
          <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
            Event Type
            <select
              className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
              value={eventType}
              onChange={(event) => setEventType(event.target.value)}
            >
              {eventTypes.map((type) => (
                <option key={type} value={type}>
                  {type === "ALL" ? "All events" : eventLabel(type)}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1 text-xs font-bold uppercase text-slate-500">
            Limit
            <input
              className="min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold normal-case text-slate-700 shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-500/20"
              type="number"
              min={1}
              max={100}
              value={limit}
              onChange={(event) => setLimit(Number(event.target.value))}
            />
          </label>
          <span className="rounded-md bg-slate-100 px-2.5 py-2 text-center text-xs font-bold text-slate-600">{events.length}</span>
        </div>
      </div>

      {isLoading ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">Loading audit events...</div>
      ) : events.length > 0 ? (
        <div className="overflow-hidden rounded-lg border border-slate-200">
          <div className="grid grid-cols-[1fr_190px_180px] bg-slate-50 px-4 py-2 text-xs font-bold uppercase text-slate-400 max-lg:hidden">
            <span>Event</span>
            <span>Operator</span>
            <span>Occurred</span>
          </div>
          <div className="divide-y divide-slate-100">
            {events.map((event) => (
              <article key={event.eventId} className="grid gap-3 px-4 py-4 lg:grid-cols-[1fr_190px_180px] lg:items-center">
                <div className="min-w-0">
                  <strong className="block truncate text-sm text-slate-950">{eventLabel(event.eventType)}</strong>
                  <span className="mt-1 block text-sm text-slate-600">{event.summary}</span>
                  {event.metadataJson ? <span className="mt-1 block truncate text-xs font-semibold text-slate-400">{formatMetadata(event.metadataJson)}</span> : null}
                </div>
                <span className="text-sm font-semibold text-slate-700">{event.operator?.displayName ?? "System"}</span>
                <time className="text-sm text-slate-500" dateTime={event.occurredAt}>
                  {new Date(event.occurredAt).toLocaleString()}
                </time>
              </article>
            ))}
          </div>
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm font-medium text-slate-500">No audit events found.</div>
      )}
    </section>
  );
}

function eventLabel(eventType: string) {
  return eventType
    .split("_")
    .map((part) => part.slice(0, 1) + part.slice(1).toLowerCase())
    .join(" ");
}

function formatMetadata(metadataJson: string) {
  try {
    const metadata = JSON.parse(metadataJson) as Record<string, string>;
    return Object.entries(metadata)
      .map(([key, value]) => `${key}: ${value}`)
      .join(" | ");
  } catch {
    return metadataJson;
  }
}
