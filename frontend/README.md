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

The verification workspace shows recent persisted sessions, allows a new session to be started, loads session detail by transaction ID, and captures manual identity details for `MANUAL_ENTRY` sessions.

## Notes

Vite uses `strictPort`, so development fails fast if port 7000 is already occupied.