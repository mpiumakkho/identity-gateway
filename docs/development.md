# Development Notes

## Prerequisites

- Java 17
- Node.js 22+
- npm 10+
- Docker Desktop or another local PostgreSQL runtime

## Maven Wrapper

The repository includes Maven Wrapper files, so a local Maven installation is not required. On macOS/Linux, replace `.\mvnw.cmd` with `./mvnw`.

## First Run

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Backend on Windows:

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Backend tests on Windows:

```powershell
.\mvnw.cmd -f backend\pom.xml test
```

Backend on macOS/Linux:

```bash
./mvnw -f backend/pom.xml spring-boot:run
./mvnw -f backend/pom.xml test
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://127.0.0.1:7000`. Vite uses `strictPort`, so it will fail fast if port 7000 is already occupied.

## Dip Chip Reader Bridge

For real card reads, run a local bridge service that wraps the selected reader SDK and exposes `POST /dip-chip/read` on `http://127.0.0.1:17520`. The frontend will use the bridge response to populate the Dip Chip form; the operator still saves the reviewed payload through the backend API.
## Conventions

- Keep package names under `com.identitygateway`.
- Use typed DTOs instead of raw JSON strings.
- Return a consistent `ApiResponse` envelope.
- Add validation annotations to request DTOs.
- Keep integration URLs and credentials in environment-specific config.
- Add tests beside domain logic as the workflow becomes concrete.
- Do not return raw sensitive identity inputs that are only needed for backend processing.
- Build each verification flow independently before connecting it to the next flow.

## Runtime Config Checks

The backend validates important runtime settings during startup. Local development can keep `DOPA_MODE=local`. When switching to `DOPA_MODE=partner`, provide `DOPA_BASE_URL`, `DOPA_VALIDATION_PATH`, and `DOPA_API_KEY` through the environment before starting the API.