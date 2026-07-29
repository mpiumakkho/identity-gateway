# Identity Gateway Frontend

React Vite staff console for identity verification operations. The UI is crafted with Tailwind CSS and Preline UI.

## Run

```bash
npm install
npm run dev
```

The dev server runs on `http://127.0.0.1:7000` and proxies `/api` to the backend.

## Build

```bash
npm run build
```

## Login Flow

The API client preserves field-level validation errors from backend responses and formats them for operator-facing messages. Authentication-required responses clear the session, while access-denied responses stay in the workspace and show an operator-facing error.

The app starts at the operator login screen. A successful login stores the issued bearer token in `sessionStorage` for the current browser tab, sends it as `Authorization: Bearer <token>` for protected API calls, and clears it on sign out or expiry. When the stored session reaches its server-issued expiry time, the workspace returns to sign-in with a session-expired notice.

The verification workspace shows system and database readiness, aggregate operations metrics, shows, filters, and sizes recent persisted sessions, looks up a transaction by ID, loads enabled intake methods from the backend catalog, allows a new session to be started, loads enriched session detail by transaction ID, captures manual identity details for `MANUAL_ENTRY` sessions, accepts normalized Dip Chip payloads for `DIP_CHIP` sessions, runs DOPA validation after identity data is captured, shows DOPA validation history, closes verified transactions with an operator decision summary, and displays the transaction audit timeline. Operators can review active sessions, revoke other sessions, and change their own password from the account security panel. Admin operators also get method catalog controls, audit inquiry, and operator management panels for event review, account creation, password changes, and disabling access.

## Dip Chip Reader Bridge

The Dip Chip panel can read card data from a localhost native bridge before saving the reviewed payload to the backend.

```text
VITE_DIP_CHIP_BRIDGE_URL=http://127.0.0.1:17520
VITE_DIP_CHIP_BRIDGE_TIMEOUT_MS=15000
```

The bridge contract is documented in `docs/dip-chip-reader-bridge.md`.
## Notes

Vite uses `strictPort`, so development fails fast if port 7000 is already occupied.


## Advanced transaction search

The Transactions panel supports method, multi-status, result limit, created-by UUID, created date range, exact national ID lookup, and CSV export through the backend report endpoint.

## Audit report export

The Audit Inquiry panel supports event type, operator UUID, result limit, and CSV export through the backend audit report endpoint.
