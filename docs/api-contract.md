# API Contract Draft

Base URL: `/api`

## System

### `GET /system/health`

Returns service health for local smoke checks.

## Authentication

### `POST /auth/login`

Initial placeholder. The endpoint returns `501 Not Implemented` until the authentication strategy is selected.

Request draft:

```json
{
  "username": "operator",
  "password": "secret"
}
```

## Verification

### `GET /verification/methods`

Returns enabled verification intake methods.

### `POST /verification/sessions`

Creates a new verification transaction session.

Request:

```json
{
  "method": "DIP_CHIP"
}
```

Response data:

```json
{
  "transactionId": "uuid",
  "method": "DIP_CHIP",
  "status": "CREATED",
  "createdAt": "2026-07-25T00:00:00Z"
}
```