# Identity Gateway

Identity Gateway is a greenfield identity verification platform for secure customer onboarding, citizen-data validation workflows, transaction tracking, and audit-ready verification operations.

## Project Layout

```text
identity-gateway/
  backend/   Spring Boot Maven API
  frontend/  React Vite staff console
  docs/      Architecture and API notes
```

## Stack

- Backend: Spring Boot 4.1, Java 17, Maven Wrapper, Spring Security, BCrypt, Spring Data JPA, Flyway
- Database: PostgreSQL for runtime, H2 only for automated tests
- Frontend: React 19, Vite, Tailwind CSS, Preline UI

## Local Development

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Backend on Windows:

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Backend on macOS/Linux:

```bash
./mvnw -f backend/pom.xml spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend dev server is locked to `http://127.0.0.1:7000` and proxies `/api` calls to `http://localhost:8080`.


## Deployment

Container deployment examples are included for PostgreSQL, the backend API, and the frontend Nginx server.

```bash
cp .env.production.example .env.production
docker compose --env-file .env.production -f compose.prod.yaml up -d --build
```

See `docs/deployment.md` for environment settings, bootstrap operator guidance, and reverse proxy notes.
## Design Principles

- New naming and package structure with no dependency on the previous project identity.
- No hard-coded salts, shared secrets, or inherited credentials in source code.
- API-first backend with clear request and response contracts.
- Frontend and backend are independently runnable.
- Domain logic should live in services, not controllers.
- Build flow by flow so each verification path can be tested independently.
