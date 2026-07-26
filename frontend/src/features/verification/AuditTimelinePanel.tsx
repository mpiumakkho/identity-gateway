import { useEffect, useState } from "react";
import { getJson } from "../../api/client";
import { isAuthenticationRequired } from "../../api/errors";
import type { AuditEvent, VerificationSession } from "./types";

type AuditTimelinePanelProps = {
  accessToken: string;
  session: VerificationSession | null;
  onError: (message: string) => void;
  onSessionExpired: () => void;
};

export function AuditTimelinePanel({ accessToken, session, onError, onSessionExpired }: AuditTimelinePanelProps) {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [localMessage, setLocalMessage] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadEvents(transactionId: string) {
      setIsLoading(true);
      setLocalMessage("");

      try {
        const response = await getJson<AuditEvent[]>(`/api/verification/sessions/${transactionId}/audit-events`, {
          accessToken
        });

        if (!cancelled) {
          setEvents(response.data ?? []);
        }
      } catch (err) {
        if (cancelled) {
          return;
        }

        if (isAuthenticationRequired(err)) {
          onSessionExpired();
          return;
        }

        const message = err instanceof Error ? err.message : "Unable to load audit timeline.";
        setLocalMessage(message);
        onError(message);
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    if (session) {
      void loadEvents(session.transactionId);
    } else {
      setEvents([]);
      setLocalMessage("");
    }

    return () => {
      cancelled = true;
    };
  }, [accessToken, onError, onSessionExpired, session?.transactionId, session?.status]);

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm" aria-labelledby="audit-title">
      <div className="mb-5 flex items-start justify-between gap-3">
        <div className="flex gap-3">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-slate-100 text-sm font-black text-slate-700">06</span>
          <div>
            <h2 id="audit-title" className="text-xl font-bold text-slate-950">Audit Timeline</h2>
            <p className="mt-1 text-sm text-slate-500">Transaction activity recorded by the platform.</p>
          </div>
        </div>
        {isLoading ? <span className="text-xs font-semibold text-slate-400">Loading</span> : null}
      </div>

      {!session ? (
        <EmptyState message="Start a session to view audit events." />
      ) : localMessage ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700" role="alert">
          {localMessage}
        </div>
      ) : events.length > 0 ? (
        <ol className="grid gap-3">
          {events.map((event) => (
            <li key={event.eventId} className="grid gap-1 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
              <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                <span className="text-sm font-bold text-slate-950">{eventLabel(event.eventType)}</span>
                <time className="text-xs font-semibold text-slate-400" dateTime={event.occurredAt}>
                  {new Date(event.occurredAt).toLocaleString()}
                </time>
              </div>
              <p className="text-sm text-slate-600">{event.summary}</p>
              <div className="flex flex-wrap gap-2 text-xs font-semibold text-slate-500">
                <span>{event.operator?.displayName ?? "System"}</span>
                {event.metadataJson ? <span className="truncate">{formatMetadata(event.metadataJson)}</span> : null}
              </div>
            </li>
          ))}
        </ol>
      ) : (
        <EmptyState message="No audit events have been recorded yet." />
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
