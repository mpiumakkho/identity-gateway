# API Contract Draft

Base URL: `/api`

All application responses use this envelope:

```json
{
  "status": "success",
  "code": "OK",
  "message": "",
  "data": {},
  "timestamp": "2026-07-25T00:00:00Z"
}
```

## System

### `GET /system/health`

Returns service health for local smoke checks.

## Authentication

### `POST /auth/login`

Authenticates an operator against a BCrypt password hash stored in PostgreSQL. A successful login returns an opaque bearer token. The raw token is returned only once; the database stores a SHA-256 token hash and expiry timestamp.

Request:

```json
{
  "username": "operator",
  "password": "secret"
}
```

Response data:

```json
{
  "operatorId": "uuid",
  "username": "operator",
  "displayName": "Operations User",
  "role": "OPERATIONS",
  "authenticatedAt": "2026-07-25T00:00:00Z",
  "accessToken": "opaque-token",
  "expiresAt": "2026-07-25T08:00:00Z"
}
```

Invalid credentials return `401` with code `INVALID_CREDENTIALS`.

## Verification

### `GET /verification/methods`

Returns enabled verification intake methods.

### `POST /verification/sessions`

Creates and persists a new verification transaction session.

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