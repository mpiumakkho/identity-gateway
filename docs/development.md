# Development Notes

## Prerequisites

- Java 17
- Maven 3.9+
- Node.js 22+
- npm 10+

## First Run

Backend:

```bash
cd backend
mvn spring-boot:run
```

Backend tests:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://127.0.0.1:7000`. Vite uses `strictPort`, so it will fail fast if port 7000 is already occupied.

## Conventions

- Keep package names under `com.identitygateway`.
- Use typed DTOs instead of raw JSON strings.
- Return a consistent `ApiResponse` envelope.
- Add validation annotations to request DTOs.
- Keep integration URLs and credentials in environment-specific config.
- Add tests beside domain logic as the workflow becomes concrete.