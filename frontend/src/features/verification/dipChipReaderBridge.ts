import type { DipChipPayload } from "./types";

const DEFAULT_BRIDGE_BASE_URL = "http://127.0.0.1:17520";
const DEFAULT_TIMEOUT_MS = 15000;

type DipChipBridgeEnvelope = {
  status?: string;
  data?: Partial<DipChipPayload>;
  error?: {
    code?: string;
    message?: string;
  };
} & Partial<DipChipPayload>;

export async function readDipChipPayloadFromBridge(): Promise<DipChipPayload> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), bridgeTimeoutMs());

  try {
    const response = await fetch(`${bridgeBaseUrl()}/dip-chip/read`, {
      method: "POST",
      headers: {
        Accept: "application/json"
      },
      signal: controller.signal
    });

    if (!response.ok) {
      throw new Error(`Reader bridge returned HTTP ${response.status}.`);
    }

    const envelope = (await response.json()) as DipChipBridgeEnvelope;
    if (envelope.status?.toLowerCase() === "error") {
      throw new Error(envelope.error?.message || "Unable to read card from reader bridge.");
    }

    return normalizeBridgePayload(envelope.data ?? envelope);
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      throw new Error("Reader bridge timed out.");
    }
    throw err;
  } finally {
    window.clearTimeout(timeout);
  }
}

function normalizeBridgePayload(payload: Partial<DipChipPayload>): DipChipPayload {
  return {
    nationalId: text(payload.nationalId).replace(/\D/g, "").slice(0, 13),
    title: text(payload.title),
    firstName: text(payload.firstName),
    lastName: text(payload.lastName),
    dateOfBirth: text(payload.dateOfBirth),
    laserCode: text(payload.laserCode).toUpperCase(),
    cardIssueDate: text(payload.cardIssueDate),
    cardExpiryDate: text(payload.cardExpiryDate),
    readerName: text(payload.readerName),
    readerSerialNumber: text(payload.readerSerialNumber).toUpperCase(),
    rawPayload: text(payload.rawPayload)
  };
}

function bridgeBaseUrl() {
  return (import.meta.env.VITE_DIP_CHIP_BRIDGE_URL || DEFAULT_BRIDGE_BASE_URL).replace(/\/$/, "");
}

function bridgeTimeoutMs() {
  const configured = Number(import.meta.env.VITE_DIP_CHIP_BRIDGE_TIMEOUT_MS);
  return Number.isFinite(configured) && configured > 0 ? configured : DEFAULT_TIMEOUT_MS;
}

function text(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}
