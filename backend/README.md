# Identity Gateway Backend

Spring Boot API service for identity verification workflows.

## Run

Start PostgreSQL first:

```bash
docker compose up -d postgres
```

Create a local operator account on first run by setting bootstrap environment variables. Keep bootstrap disabled after the account exists.

```bash
set BOOTSTRAP_OPERATOR_ENABLED=true
set BOOTSTRAP_OPERATOR_USERNAME=operator
set BOOTSTRAP_OPERATOR_PASSWORD=Change-this-password-123
set BOOTSTRAP_OPERATOR_DISPLAY_NAME=Operations User
```

Then run the API:

```bash
..\mvnw.cmd -f pom.xml spring-boot:run
```

## Test

```bash
..\mvnw.cmd -f pom.xml test
```

Tests use the `test` profile with an in-memory H2 database. Runtime configuration targets PostgreSQL.

## Configuration

Runtime database settings are read from environment variables:

```text
POSTGRES_URL=jdbc:postgresql://localhost:5432/identity_gateway
POSTGRES_USER=identity_gateway
POSTGRES_PASSWORD=identity_gateway
APP_CORS_ALLOWED_ORIGINS=http://127.0.0.1:7000,http://localhost:7000
```

Authentication settings:

```text
BOOTSTRAP_OPERATOR_ENABLED=false
BOOTSTRAP_OPERATOR_USERNAME=operator
BOOTSTRAP_OPERATOR_PASSWORD=
BOOTSTRAP_OPERATOR_DISPLAY_NAME=Operations User
BOOTSTRAP_OPERATOR_ROLE=OPERATIONS
```

Authentication hardening settings:

```text
AUTH_LOCKOUT_ENABLED=true
AUTH_LOCKOUT_MAX_FAILED_ATTEMPTS=5
AUTH_LOCKOUT_DURATION=PT15M
AUTH_PASSWORD_MIN_LENGTH=12
AUTH_PASSWORD_MAX_LENGTH=128
AUTH_PASSWORD_REQUIRE_UPPERCASE=true
AUTH_PASSWORD_REQUIRE_LOWERCASE=true
AUTH_PASSWORD_REQUIRE_DIGIT=true
AUTH_PASSWORD_REQUIRE_SPECIAL=false
AUTH_SESSION_CLEANUP_ENABLED=true
AUTH_SESSION_CLEANUP_RETENTION=P30D
AUTH_SESSION_CLEANUP_FIXED_DELAY=PT1H
```

Observability settings:

```text
# Actuator exposes health, info, and metrics. Health probes are public; metrics use normal authentication.
# Logs include X-Request-Id as requestId. Clients may send X-Request-Id or let the API generate one.
```
DOPA integration settings:

```text
DOPA_MODE=local
DOPA_BASE_URL=
DOPA_VALIDATION_PATH=/validate
DOPA_API_KEY=
DOPA_CONNECT_TIMEOUT=PT3S
DOPA_READ_TIMEOUT=PT10S
DOPA_RETRY_ATTEMPTS=1
```

Use `DOPA_MODE=partner` only when the partner endpoint and credentials are supplied through the runtime environment.

## Initial Endpoints

- `GET /api/system/health` with service and database readiness
- `GET /actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` for runtime probes
- `GET /actuator/info` and authenticated `/actuator/metrics` for runtime observability
- `POST /api/auth/login`
- `GET /api/verification/methods` from the enabled method catalog
- `GET /api/verification/methods/catalog` for admin catalog management
- `PUT /api/verification/methods/{methodId}/enabled` for admin method enablement
- `GET /api/verification/sessions` with optional `method`, comma-separated `status`, `createdBy`, `createdFrom`, `createdTo`, and exact `identityNationalId` filters
- `GET /api/verification/reports/sessions.csv` for transaction CSV export with the same session filters
- `GET /api/audit-events/report.csv` for audit CSV export
- `GET /api/verification/sessions/{transactionId}` with masked workflow summaries
- `POST /api/verification/sessions`
- `PUT /api/verification/sessions/{transactionId}/manual-identity`
- `PUT /api/verification/sessions/{transactionId}/dip-chip-payload`
- `POST /api/verification/sessions/{transactionId}/dopa-validation`
- `POST /api/verification/sessions/{transactionId}/closeout`
- `GET /api/verification/sessions/{transactionId}/audit-events`

## Security Notes

Passwords are verified with Spring Security `PasswordEncoder` backed by BCrypt. Login failed-attempt lockout, password policy rules, and old-session cleanup are configurable through runtime environment variables. Admin operator management endpoints also hash new passwords with BCrypt and revoke active sessions after password changes or account disabling. Login returns an opaque bearer token while PostgreSQL stores only the token hash and expiry. Application endpoints reject missing, expired, revoked, or invalid tokens with `AUTHENTICATION_REQUIRED`. Do not commit passwords, salts, signing keys, API tokens, or partner credentials to the repository.


Configuration validation:

The API validates runtime configuration during startup and fails fast with a clear error when required settings are missing or invalid. Important checks include positive session/lockout/cleanup durations, password policy min/max consistency, at least one CORS origin, and required DOPA partner settings when `DOPA_MODE=partner`.
## Observability

Actuator health probes are enabled for infrastructure checks:

- `/actuator/health/liveness`
- `/actuator/health/readiness`

Business metrics are registered through Micrometer with names such as `identity_gateway_verification_sessions`, `identity_gateway_verification_sessions_by_method`, `identity_gateway_operator_sessions_active`, and `identity_gateway_operators_locked`. Request logs include a correlation value from `X-Request-Id` when supplied, or a generated request ID when missing.
