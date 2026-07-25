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

### `GET /auth/sessions`

Returns active bearer-token sessions for the current operator. The response marks the session matching the current bearer token.

Response data:

```json
[
  {
    "sessionId": "uuid",
    "current": true,
    "createdAt": "2026-07-25T00:00:00Z",
    "expiresAt": "2026-07-25T08:00:00Z"
  }
]
```

### `DELETE /auth/sessions/{sessionId}`

Revokes one active session belonging to the current operator. The current bearer-token session cannot be revoked through this endpoint; use logout for the current session.

Response data:

```json
{
  "revoked": true
}
```

Unknown session IDs return `404` with code `NOT_FOUND`.

### `PUT /auth/password`

Changes the current operator password after verifying the current password. The current bearer token remains active and other active sessions for the same operator are revoked.

Request:

```json
{
  "currentPassword": "current-secret-123",
  "newPassword": "new-secret-123"
}
```

Response data:

```json
{
  "passwordChanged": true
}
```

An incorrect current password returns `401` with code `INVALID_CREDENTIALS`. Missing, expired, revoked, or invalid tokens return `401` with code `AUTHENTICATION_REQUIRED`.

### `POST /auth/logout`

Revokes the current bearer token.

Response data:

```json
{
  "signedOut": true
}
```

Missing, expired, revoked, or invalid tokens return `401` with code `AUTHENTICATION_REQUIRED`.

## Operator Management

All operator management endpoints require an authenticated `ADMIN` operator. Passwords are accepted only in request bodies and are never returned. Password changes and disabled accounts revoke active sessions for the affected operator.

### `GET /operators`

Returns operator accounts ordered by newest first.

Response data:

```json
[
  {
    "operatorId": "uuid",
    "username": "operator",
    "displayName": "Operations User",
    "role": "OPERATIONS",
    "enabled": true,
    "createdAt": "2026-07-25T00:00:00Z",
    "updatedAt": "2026-07-25T00:00:00Z",
    "disabledAt": null
  }
]
```

### `POST /operators`

Creates an enabled operator account with a BCrypt password hash.

Request:

```json
{
  "username": "operator",
  "password": "very-secret-123",
  "displayName": "Operations User",
  "role": "OPERATIONS"
}
```

Duplicate usernames return `400` with code `BAD_REQUEST`.

### `PUT /operators/{operatorId}/password`

Changes an operator password and revokes that operator's active sessions.

Request:

```json
{
  "password": "new-secret-123"
}
```

### `PUT /operators/{operatorId}/disabled`

Disables an operator account and revokes active sessions. Admin operators cannot disable their own account. Unknown IDs return `404` with code `NOT_FOUND`.
## Audit Inquiry

### `GET /audit-events`

Returns recent platform audit events for authenticated `ADMIN` operators. Optional query parameters: `eventType`, `operatorId`, and `limit` from `1` to `100`.

Response data:

```json
[
  {
    "eventId": "uuid",
    "eventType": "AUTH_LOGIN_SUCCEEDED",
    "transactionId": null,
    "operator": {
      "operatorId": "uuid",
      "username": "operator",
      "displayName": "Operations User"
    },
    "summary": "Operator login succeeded.",
    "metadataJson": "{\"username\":\"operator\"}",
    "occurredAt": "2026-07-25T00:00:00Z"
  }
]
```

Unsupported event types return `400` with code `BAD_REQUEST`.
## Verification

### `GET /verification/dashboard`

Returns aggregate transaction metrics for the operator console. Requires authentication.

Response data:

```json
{
  "totalTransactions": 3,
  "byStatus": [
    { "key": "CREATED", "count": 1 },
    { "key": "APPROVED", "count": 2 }
  ],
  "byMethod": [
    { "key": "DIP_CHIP", "count": 2 },
    { "key": "MANUAL_ENTRY", "count": 1 }
  ]
}
```
### `GET /verification/methods`

Returns enabled verification intake methods. Requires authentication.

### `GET /verification/sessions`

Returns the latest 20 persisted verification sessions. Requires authentication. Optional query parameters: `method` (`DIP_CHIP`, `MANUAL_ENTRY`) and `status` (`CREATED`, `IDENTITY_CAPTURED`, `DOPA_VERIFIED`, `DOPA_REJECTED`, `APPROVED`, `REJECTED`).

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

Returns one verification session by transaction ID. Requires authentication. Unknown IDs return `404` with code `NOT_FOUND`. The response includes optional masked workflow summaries when identity capture, DOPA validation, or closeout has been completed. Sensitive values such as national ID, laser code, raw Dip Chip payloads, passwords, and bearer tokens are not returned.

Response data:

```json
{
  "transactionId": "uuid",
  "method": "DIP_CHIP",
  "status": "APPROVED",
  "createdBy": {
    "operatorId": "uuid",
    "username": "operator",
    "displayName": "Operations User"
  },
  "createdAt": "2026-07-25T00:00:00Z",
  "identity": {
    "source": "DIP_CHIP",
    "maskedNationalId": "123******0121",
    "title": "Mr.",
    "firstName": "Somchai",
    "lastName": "Jaidee",
    "dateOfBirth": "1990-01-31",
    "cardIssueDate": "2021-02-01",
    "cardExpiryDate": "2031-01-31",
    "readerName": "ACR39U",
    "readerSerialNumber": "RD-001",
    "updatedAt": "2026-07-25T01:20:00Z"
  },
  "dopaValidation": {
    "validationStatus": "MATCHED",
    "identitySource": "DIP_CHIP",
    "responseCode": "DOPA-0000",
    "responseMessage": "Citizen identity matched.",
    "consentReference": "CONSENT-001",
    "validatedAt": "2026-07-25T01:30:00Z"
  },
  "closeout": {
    "decision": "APPROVED",
    "notes": "Matched and reviewed.",
    "decidedBy": {
      "operatorId": "uuid",
      "username": "operator",
      "displayName": "Operations User"
    },
    "decidedAt": "2026-07-25T02:00:00Z"
  }
}
```

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

Captures or updates citizen-card details for a `MANUAL_ENTRY` transaction session. Requires authentication. The session moves to `IDENTITY_CAPTURED` when the identity payload is saved. Text fields are normalized before persistence. `laserCode` is accepted for controlled capture but is not returned in API responses.

Request:

```json
{
  "nationalId": "1234567890121",
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
  "maskedNationalId": "123******0121",
  "title": "Mr.",
  "firstName": "Somchai",
  "lastName": "Jaidee",
  "dateOfBirth": "1990-01-31",
  "updatedAt": "2026-07-25T00:00:00Z"
}
```

Invalid payloads return `400` with code `VALIDATION_ERROR`. Non-manual sessions return `400` with code `BAD_REQUEST`.

### `PUT /verification/sessions/{transactionId}/dip-chip-payload`

Captures or updates normalized citizen-card payload data for a `DIP_CHIP` transaction session. Requires authentication. The session moves to `IDENTITY_CAPTURED` when the card payload is saved. Text fields are normalized before persistence, and card expiry must be on or after the issue date. `laserCode` and `rawPayload` are accepted for backend processing and audit traceability but are not returned in API responses.

Request:

```json
{
  "nationalId": "1234567890121",
  "title": "Mr.",
  "firstName": "Somchai",
  "lastName": "Jaidee",
  "dateOfBirth": "1990-01-31",
  "laserCode": "JT1234567890",
  "cardIssueDate": "2021-02-01",
  "cardExpiryDate": "2031-01-31",
  "readerName": "ACR39U",
  "readerSerialNumber": "RD-001",
  "rawPayload": "CID=1234567890121;READER=ACR39U"
}
```

Response data:

```json
{
  "transactionId": "uuid",
  "sessionStatus": "IDENTITY_CAPTURED",
  "maskedNationalId": "123******0121",
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

### `GET /verification/sessions/{transactionId}/dopa-validations`

Returns the latest 10 DOPA validation attempts for a transaction. Requires authentication. The response does not include national ID, laser code, or raw identity payloads.

Response data:

```json
[
  {
    "attemptId": "uuid",
    "validationStatus": "MATCHED",
    "identitySource": "DIP_CHIP",
    "responseCode": "DOPA-0000",
    "responseMessage": "Citizen identity matched.",
    "consentReference": "CONSENT-001",
    "validatedAt": "2026-07-25T01:30:00Z"
  }
]
```

Unknown transaction IDs return `404` with code `NOT_FOUND`.
### `POST /verification/sessions/{transactionId}/dopa-validation`

Validates the captured identity details for a transaction session. Requires authentication. The session must already be `IDENTITY_CAPTURED`, `DOPA_VERIFIED`, or `DOPA_REJECTED`. A matched result moves the session to `DOPA_VERIFIED`; an unmatched result moves it to `DOPA_REJECTED`. The response masks the national ID and never returns the laser code.

Request:

```json
{
  "consentReference": "CONSENT-001"
}
```

Response data:

```json
{
  "transactionId": "uuid",
  "sessionStatus": "DOPA_VERIFIED",
  "validationStatus": "MATCHED",
  "identitySource": "DIP_CHIP",
  "maskedNationalId": "123******0121",
  "responseCode": "DOPA-0000",
  "responseMessage": "Citizen identity matched.",
  "consentReference": "CONSENT-001",
  "validatedAt": "2026-07-25T00:00:00Z"
}
```

Invalid payloads return `400` with code `VALIDATION_ERROR`. Sessions without captured identity return `400` with code `BAD_REQUEST`.

### `POST /verification/sessions/{transactionId}/closeout`

Closes a transaction after DOPA validation and records the operator decision. Requires authentication. The session must be `DOPA_VERIFIED` or `DOPA_REJECTED`. `DOPA_REJECTED` sessions cannot be approved. A successful closeout moves the session to `APPROVED` or `REJECTED` and prevents further identity capture or DOPA validation.

Request:

```json
{
  "decision": "APPROVED",
  "notes": "Matched and reviewed."
}
```

Response data:

```json
{
  "transactionId": "uuid",
  "sessionStatus": "APPROVED",
  "decision": "APPROVED",
  "notes": "Matched and reviewed.",
  "decidedBy": {
    "operatorId": "uuid",
    "username": "operator",
    "displayName": "Operations User"
  },
  "decidedAt": "2026-07-25T00:00:00Z"
}
```

Invalid payloads return `400` with code `VALIDATION_ERROR`. Transactions that are not ready for closeout return `400` with code `BAD_REQUEST`.

### `GET /verification/sessions/{transactionId}/audit-events`

Returns the audit timeline for one transaction. Requires authentication. Audit metadata is intentionally limited to workflow-safe fields and excludes national ID, laser code, raw Dip Chip payloads, passwords, and bearer tokens.

Response data:

```json
[
  {
    "eventId": "uuid",
    "eventType": "VERIFICATION_SESSION_CREATED",
    "transactionId": "uuid",
    "operator": {
      "operatorId": "uuid",
      "username": "operator",
      "displayName": "Operations User"
    },
    "summary": "Verification session created.",
    "metadataJson": "{\"method\":\"DIP_CHIP\",\"status\":\"CREATED\"}",
    "occurredAt": "2026-07-25T00:00:00Z"
  }
]
```
