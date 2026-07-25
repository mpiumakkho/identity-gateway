# Identity Gateway Backend

Spring Boot API service for identity verification workflows.

## Run

Start PostgreSQL first:

```bash
docker compose up -d postgres
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

## Initial Endpoints

- `GET /api/system/health`
- `POST /api/auth/login`
- `GET /api/verification/methods`
- `POST /api/verification/sessions`

## Security Notes

Passwords are verified with Spring Security `PasswordEncoder` backed by BCrypt. Do not commit passwords, salts, signing keys, API tokens, or partner credentials to the repository.