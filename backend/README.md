# Identity Gateway Backend

Spring Boot API service for identity verification workflows.

## Run

```bash
mvn spring-boot:run
```

## Test

```bash
mvn test
```

## Initial Endpoints

- `GET /api/system/health`
- `GET /api/verification/methods`
- `POST /api/verification/sessions`
- `POST /api/auth/login` returns `501 Not Implemented` until the authentication strategy is added.

## Notes

Keep secrets out of source code. Use environment variables or deployment secrets for credentials, salts, API tokens, and signing keys.