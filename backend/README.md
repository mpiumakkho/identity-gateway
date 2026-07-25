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
set BOOTSTRAP_OPERATOR_PASSWORD=change-this-password
set BOOTSTRAP_OPERATOR_DISPLAY_NAME=Operations User
```

Then run the API:

```bash
mvn spring-boot:run
```

## Test

```bash
mvn test
```

Tests use the `test` profile with an in-memory H2 database. Runtime configuration targets PostgreSQL.

## Configuration

Runtime database settings are read from environment variables:

```text
POSTGRES_URL=jdbc:postgresql://localhost:5432/identity_gateway
POSTGRES_USER=identity_gateway
POSTGRES_PASSWORD=identity_gateway
```

Authentication settings:

```text
BOOTSTRAP_OPERATOR_ENABLED=false
BOOTSTRAP_OPERATOR_USERNAME=operator
BOOTSTRAP_OPERATOR_PASSWORD=
BOOTSTRAP_OPERATOR_DISPLAY_NAME=Operations User
BOOTSTRAP_OPERATOR_ROLE=OPERATIONS
```

## Initial Endpoints

- `GET /api/system/health` with service and database readiness
- `POST /api/auth/login`
- `GET /api/verification/methods` from the persisted method catalog
- `GET /api/verification/sessions` with optional `method` and `status` filters
- `GET /api/verification/sessions/{transactionId}` with masked workflow summaries
- `POST /api/verification/sessions`
- `PUT /api/verification/sessions/{transactionId}/manual-identity`
- `PUT /api/verification/sessions/{transactionId}/dip-chip-payload`
- `POST /api/verification/sessions/{transactionId}/dopa-validation`
- `POST /api/verification/sessions/{transactionId}/closeout`
- `GET /api/verification/sessions/{transactionId}/audit-events`

## Security Notes

Passwords are verified with Spring Security `PasswordEncoder` backed by BCrypt. Admin operator management endpoints also hash new passwords with BCrypt and revoke active sessions after password changes or account disabling. Login returns an opaque bearer token while PostgreSQL stores only the token hash and expiry. Application endpoints reject missing, expired, revoked, or invalid tokens with `AUTHENTICATION_REQUIRED`. Do not commit passwords, salts, signing keys, API tokens, or partner credentials to the repository.
