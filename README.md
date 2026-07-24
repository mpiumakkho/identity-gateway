# Identity Gateway

Identity Gateway is a greenfield identity verification platform for secure customer onboarding, citizen-data validation workflows, transaction tracking, and audit-ready verification operations.

## Project Layout

```text
identity-gateway/
  backend/   Spring Boot Maven API
  frontend/  React Vite staff console
  docs/      Architecture and API notes
```

## Local Development

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend dev server is locked to `http://127.0.0.1:7000` and proxies `/api` calls to `http://localhost:8080`.

## Design Principles

- New naming and package structure with no dependency on the previous project identity.
- No hard-coded salts, shared secrets, or inherited credentials in source code.
- API-first backend with clear request and response contracts.
- Frontend and backend are independently runnable.
- Domain logic should live in services, not controllers.