# Architecture

Identity Gateway is split into two independently runnable applications.

## Backend

The backend exposes REST APIs under `/api`. Controllers should stay thin and delegate business rules to service classes as the domain grows.

Planned backend modules:

- `auth`: authentication and session/token strategy
- `verification`: verification workflow orchestration
- `identity`: citizen identity models and validation
- `transaction`: transaction persistence and status history
- `audit`: audit events and operator activity logs
- `config`: application configuration, CORS, security, and integration clients
- `common`: shared API response and error handling

## Frontend

The frontend is a React Vite app for staff operations. Feature code should live under `src/features`, shared API helpers under `src/api`, and shared styling under `src/styles`.

## Data Handling

Do not store verification transaction state in static in-memory fields. Use a database, cache, or explicit session store once persistence is added.

## Secrets

No salts, passwords, signing keys, API tokens, or partner credentials should be committed to the repository.