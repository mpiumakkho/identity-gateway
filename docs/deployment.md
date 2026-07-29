# Deployment

This repository includes container examples for running the full application stack with PostgreSQL, the Spring Boot API, and the React console served by Nginx.

## Files

- `compose.prod.yaml`: production-oriented Compose stack for PostgreSQL, backend, and frontend.
- `.env.production.example`: environment template for Compose deployment values.
- `backend/Dockerfile`: multi-stage Java 17 backend image build.
- `frontend/Dockerfile`: multi-stage React build served by Nginx.
- `frontend/nginx.conf`: static frontend hosting with `/api` and health proxy routes to the backend container.

## Run The Stack

Create a deployment environment file from the example and set real values before starting the stack.

```bash
cp .env.production.example .env.production
```

Start the stack:

```bash
docker compose --env-file .env.production -f compose.prod.yaml up -d --build
```

Check health:

```bash
docker compose --env-file .env.production -f compose.prod.yaml ps
curl http://localhost/actuator/health/readiness
```

The frontend listens on `FRONTEND_PORT`, defaulting to `80`. Browser requests to `/api` are proxied to the backend service inside the Compose network.

## Bootstrap Operator

For first deployment only, set these values in `.env.production`:

```text
BOOTSTRAP_OPERATOR_ENABLED=true
BOOTSTRAP_OPERATOR_USERNAME=admin
BOOTSTRAP_OPERATOR_PASSWORD=<set-a-strong-temporary-password>
BOOTSTRAP_OPERATOR_ROLE=ADMIN
```

After the first admin operator is created, set `BOOTSTRAP_OPERATOR_ENABLED=false` and redeploy so new bootstrap accounts cannot be created accidentally.

## Reverse Proxy

If a platform reverse proxy or load balancer sits in front of Compose, terminate TLS there and forward traffic to the frontend container. Keep `APP_CORS_ALLOWED_ORIGINS` aligned with the public HTTPS origin.

Example external proxy target:

```text
https://identity-gateway.example.com -> http://frontend:80
```

Nginx inside the frontend container forwards `/api/*` and `/actuator/health/*` to the backend container. Do not expose the backend port publicly unless an environment requires a separate API entry point.

## Production Settings

Set non-default values for:

- `POSTGRES_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `DOPA_MODE`, `DOPA_BASE_URL`, `DOPA_VALIDATION_PATH`, and `DOPA_API_KEY` when using partner validation
- password policy and lockout settings when the deployment policy differs from defaults

Do not commit filled `.env.production` files or partner credentials.
