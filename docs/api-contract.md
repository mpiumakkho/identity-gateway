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

### `PUT /verification/sessions/{transactionId}/manual-identity`

Captures or updates citizen-card details for a `MANUAL_ENTRY` transaction session. Requires authentication. The session moves to `IDENTITY_CAPTURED` when the identity payload is saved. `laserCode` is accepted for controlled capture but is not returned in API responses.

Request:

```json
{
  "nationalId": "1234567890123",
  "title": "Mr.",
  "firstName": "Somchai",
  "lastName": "Jaidee",
  "dateOfBirth": "1990-01-31",
  "laserCode": "JT1234567890"
}
```

Response data:

```json
{
  "transactionId": "uuid",
  "sessionStatus": "IDENTITY_CAPTURED",
  "maskedNationalId": "123******0123",
  "title": "Mr.",
  "firstName": "Somchai",
  "lastName": "Jaidee",
  "dateOfBirth": "1990-01-31",
  "updatedAt": "2026-07-25T00:00:00Z"
}
```

Invalid payloads return `400` with code `VALIDATION_ERROR`. Non-manual sessions return `400` with code `BAD_REQUEST`.

### `PUT /verification/sessions/{transactionId}/dip-chip-payload`

Captures or updates normalized citizen-card payload data for a `DIP_CHIP` transaction session. Requires authentication. The session moves to `IDENTITY_CAPTURED` when the card payload is saved. `laserCode` and `rawPayload` are accepted for backend processing and audit traceability but are not returned in API responses.

Request:

```json
{
  "nationalId": "1234567890123",
  "title": "Mr.",
  "firstName": "Somchai",
  "lastName": "Jaidee",
  "dateOfBirth": "1990-01-31",
  "laserCode": "JT1234567890",
  "cardIssueDate": "2021-02-01",
  "cardExpiryDate": "2031-01-31",
  "readerName": "ACR39U",
  "readerSerialNumber": "RD-001",
  "rawPayload": "CID=1234567890123;READER=ACR39U"
}
```

Response data:

```json
{
  "transactionId": "uuid",
  "sessionStatus": "IDENTITY_CAPTURED",
  "maskedNationalId": "123******0123",
  "title": "Mr.",
  "firstName": "Somchai",
  "lastName": "Jaidee",
  "dateOfBirth": "1990-01-31",
  "cardIssueDate": "2021-02-01",
  "cardExpiryDate": "2031-01-31",
  "readerName": "ACR39U",
  "readerSerialNumber": "RD-001",
  "updatedAt": "2026-07-25T00:00:00Z"
}
```

Invalid payloads return `400` with code `VALIDATION_ERROR`. Non-Dip-Chip sessions return `400` with code `BAD_REQUEST`.
