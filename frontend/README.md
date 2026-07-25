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

The app starts at the operator login screen. A successful login stores the issued bearer token in `sessionStorage` for the current browser tab, sends it as `Authorization: Bearer <token>` for protected API calls, and clears it on sign out or expiry.

The verification workspace shows and filters recent persisted sessions, allows a new session to be started, loads enriched session detail by transaction ID, captures manual identity details for `MANUAL_ENTRY` sessions, accepts normalized Dip Chip payloads for `DIP_CHIP` sessions, runs DOPA validation after identity data is captured, closes verified transactions with an operator decision summary, and displays the transaction audit timeline. Operators can review active sessions, revoke other sessions, and change their own password from the account security panel. Admin operators also get an operator management console for account creation, password changes, and disabling access.

## Notes

Vite uses `strictPort`, so development fails fast if port 7000 is already occupied.
