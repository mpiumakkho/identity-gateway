# Architecture

Identity Gateway is split into two independently runnable applications.

## Backend

The backend exposes REST APIs under `/api`. Controllers stay thin and delegate business rules to service classes.

Current backend modules:

- `auth`: operator login, user repository, BCrypt password verification, active session controls, self-service password changes, and admin operator management
- `verification`: persisted verification method catalog, system health, dashboard metrics, persisted session creation, transaction inquiry, enriched transaction detail, manual identity capture, Dip Chip payload intake, and decision closeout
- `dipchip`: citizen-card reader payload normalization and card-date consistency checks
- `identity`: shared citizen identity data protection, masking, validation, and manual identity normalization helpers
- `dopa`: citizen registry validation, result persistence, validation history, and audit-safe response mapping
- `audit`: persisted operator, authentication, and transaction audit timeline with admin inquiry
- `config`: CORS, Spring Security configuration, request correlation, runtime metrics, and startup configuration validation
- `common`: shared API response and error handling
- `report`: CSV report rendering with safe escaping for export endpoints


## Database

Runtime uses PostgreSQL. Schema changes are managed by Flyway migrations in `backend/src/main/resources/db/migration`.

Automated tests use the `test` profile with H2 so unit and slice tests do not require a local PostgreSQL server.

## Frontend

The frontend is a React Vite app for staff operations. Styling uses Tailwind CSS with Preline UI components. Feature code lives under `src/features`, shared API helpers under `src/api`, and shared styling under `src/styles`. Admin-only operator management UI lives under `src/features/operators`.

Preline is loaded with a dynamic import after React mounts so the main application bundle stays lighter.

## Flow Roadmap

1. Foundation flow: PostgreSQL, Flyway, BCrypt auth foundation, verification session persistence.
2. Login flow: operator login, session/token strategy, protected app shell.
3. Method flow: load enabled verification methods from the backend catalog, select `DIP_CHIP` or `MANUAL_ENTRY`, and create a transaction session.
4. Manual identity flow: controlled citizen-data entry and validation.
5. Dip Chip flow: card-reader payload capture and normalization. Done as a dedicated intake flow before DOPA integration.
6. DOPA flow: citizen registry validation request, response mapping, and verified/rejected session status. Done with local mode for development and a configurable partner HTTP adapter for real integration.
7. Summary flow: verification decision and transaction closeout. Done with persisted operator decision records.
8. Audit flow: operator and transaction event timeline. Done with persisted audit events and a transaction timeline view.
9. Transaction inquiry flow: method and status filtering for persisted verification sessions. Done with backend query filters and console controls.
10. Transaction detail flow: masked identity, DOPA, and closeout summaries. Done with enriched detail responses, direct transaction ID lookup, and console summary sections.
11. Operator management flow: admin operator listing, account creation, password changes, and disabling. Done with BCrypt hashing, session revocation, audit events, and an admin console panel.
12. Account security flow: self-service password changes for authenticated operators. Done with current-password verification, BCrypt rehashing, other-session revocation, audit events, and a console panel.
13. Active sessions flow: current-operator session listing and other-session revocation. Done with token-hash matching, ownership checks, audit events, and account security controls.
14. Audit inquiry flow: admin review of recent audit events across authentication, operator management, sessions, and verification transactions. Done with event filters, capped limits, and console controls.
15. Operations dashboard flow: aggregate verification transaction metrics by status and intake method. Done with grouped database queries and console summary cards.
16. DOPA validation history flow: latest validation attempts per transaction. Done with safe attempt responses, transaction-scoped endpoint, and console history list.
17. System health flow: service and database readiness for operators. Done with a DB-backed health response and console refresh panel.
18. Persisted method catalog flow: verification method options stored in PostgreSQL and served from the catalog. Done with Flyway seed data, repository-backed catalog responses, and disabled-method enforcement on session creation.
19. Method catalog management flow: admin control of enabled intake methods. Done with admin catalog endpoints, audit events, security rules, and console toggles.
20. Identity data protection flow: shared masking for citizen identity data. Done with a common identity helper used by verification and DOPA responses.
21. National ID validation flow: checksum validation for captured citizen IDs. Done with shared backend validation annotations, request-level enforcement, tests, and client-side pre-submit checks.
22. Dip Chip normalization flow: reader payload normalization before persistence. Done with a dedicated module, normalized payload mapping, card-date consistency checks, and focused unit coverage.
23. Manual identity normalization flow: manual-entry identity normalization before persistence. Done with shared text normalization, normalized identity mapping, and focused unit coverage.
24. Validation error detail flow: field-level validation details in API error responses. Done with an envelope-level errors field and MVC coverage.
25. Frontend validation error handling flow: field-level validation details consumed by the React API client. Done with typed error payloads, formatted operator messages, and envelope consistency for authentication failures.
26. Authentication error envelope flow: unauthorized responses serialized through the shared API envelope. Done with the common response factory and security integration coverage.
27. Authorization error envelope flow: forbidden responses serialized through the shared API envelope. Done with a REST access-denied handler and security integration coverage.
28. Frontend authorization handling flow: authentication expiry and access denial are handled separately in the React workspace. Done with shared API error helpers and operator-facing forbidden messages.
29. Frontend session expiry flow: the React shell clears expired browser sessions on schedule and returns operators to sign-in with a session-expired notice.
30. Transaction inquiry limit flow: recent verification sessions support an operator-selected result limit capped by the backend.
31. Build automation flow: Maven Wrapper and GitHub Actions run backend tests and frontend builds without requiring a local Maven installation.
32. DOPA partner integration flow: DOPA validation can run through a configurable partner HTTP adapter with environment-provided credentials, timeout/retry controls, and technical-failure mapping while local mode remains available for development and tests.
33. Dip Chip reader bridge flow: the frontend can call a localhost native bridge for real card-reader SDK access, populate the reviewed payload form, and submit through the existing backend persistence/audit boundary.
34. Permission granularity flow: operator roles now map to explicit permissions used by backend authorization rules and frontend admin navigation.
35. Session admin flow: admins can list and revoke active sessions for managed operators while preserving the current admin session during bulk self-revocation.
36. Production hardening flow: configurable login lockout, password policy enforcement, bootstrap password validation, and scheduled expired/revoked session cleanup.
37. Observability flow: actuator liveness/readiness probes, authenticated metrics exposure, request correlation IDs, structured console logs, and Micrometer business gauges.
38. Config validation flow: startup fail-fast checks for DOPA partner credentials, auth duration bounds, password policy consistency, session cleanup timing, and CORS origins.
39. Export/report flow: CSV downloads for filtered verification sessions and audit events with safe CSV escaping and attachment headers.
40. Advanced search flow: verification sessions and CSV reports support date ranges, created-by filters, multi-status combinations, and exact national-ID search while keeping response identity data masked.
41. Deployment flow: container build files, production Compose stack, environment example, frontend Nginx proxy, and deployment notes for bootstrap and reverse proxy operation.
42. Test/runtime polish flow: quieter test logging and roadmap wording aligned with implemented DOPA partner integration.
43. Frontend transaction search flow: operator console exposes advanced session filters and CSV download using the backend search/export APIs.
44. Frontend audit export flow: admin audit inquiry exposes operator filtering and CSV download using the backend audit report API.
45. Frontend report download helper flow: CSV report downloads share one authenticated download helper with consistent session-expiry handling.

## Data Handling

Do not store verification transaction state in static in-memory fields. Persist workflow state in PostgreSQL and keep short-lived client state in the frontend only when it can be safely recreated.

## Secrets

No salts, passwords, signing keys, API tokens, or partner credentials should be committed to the repository. Partner credentials must be supplied through runtime environment variables.
