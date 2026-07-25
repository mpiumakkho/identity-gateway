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

- `GET /api/system/health`
- `POST /api/auth/login`
- `GET /api/verification/methods`
- `GET /api/verification/sessions`
- `GET /api/verification/sessions/{transactionId}`
- `POST /api/verification/sessions`
- `PUT /api/verification/sessions/{transactionId}/manual-identity`

## Security Notes

Passwords are verified with Spring Security `PasswordEncoder` backed by BCrypt. Login returns an opaque bearer token while PostgreSQL stores only the token hash and expiry. Application endpoints reject missing, expired, revoked, or invalid tokens with `AUTHENTICATION_REQUIRED`. Do not commit passwords, salts, signing keys, API tokens, or partner credentials to the repository.