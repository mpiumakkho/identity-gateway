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

Protected endpoints require an opaque bearer token:

```http
Authorization: Bearer <accessToken>
```

## System

### `GET /system/health`

Returns service health for local smoke checks. This endpoint is public.

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

### `GET /auth/me`

Returns the current operator for a valid bearer token.

Response data:

```json
{
  "operatorId": "uuid",
  "username": "operator",
  "displayName": "Operations User",
  "role": "OPERATIONS",
  "sessionExpiresAt": "2026-07-25T08:00:00Z"
}
```

### `POST /auth/logout`

Revokes the current bearer token.

Response data:

```json
{
  "signedOut": true
}
```

Missing, expired, revoked, or invalid tokens return `401` with code `AUTHENTICATION_REQUIRED`.

## Verification

### `GET /verification/methods`

Returns enabled verification intake methods. Requires authentication.

### `GET /verification/sessions`

Returns the latest 20 persisted verification sessions. Requires authentication.

Response data:

```json
[
  {
    "transactionId": "uuid",
    "method": "DIP_CHIP",
    "status": "CREATED",
    "createdBy": {
      "operatorId": "uuid",
      "username": "operator",
      "displayName": "Operations User"
    },
    "createdAt": "2026-07-25T00:00:00Z"
  }
]
```

### `GET /verification/sessions/{transactionId}`

Returns one verification session by transaction ID. Requires authentication. Unknown IDs return `404` with code `NOT_FOUND`.

### `POST /verification/sessions`

Creates and persists a new verification transaction session for the authenticated operator. Requires authentication.

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
  "createdBy": {
    "operatorId": "uuid",
    "username": "operator",
    "displayName": "Operations User"
  },
  "createdAt": "2026-07-25T00:00:00Z"
}
```