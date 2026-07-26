# Dip Chip Reader Bridge

The web console reads citizen-card data through a local native bridge. The bridge is a small desktop service owned by the reader SDK integration and reachable from the browser on localhost.

## Browser Request

Default URL:

```http
POST http://127.0.0.1:17520/dip-chip/read
Accept: application/json
```

Frontend configuration:

```text
VITE_DIP_CHIP_BRIDGE_URL=http://127.0.0.1:17520
VITE_DIP_CHIP_BRIDGE_TIMEOUT_MS=15000
```

The bridge must allow CORS requests from the frontend origin, normally `http://127.0.0.1:7000` in development.

## Successful Response

The bridge may return either a plain payload or an envelope with `status` and `data`. Dates must use ISO `yyyy-MM-dd`.

```json
{
  "status": "success",
  "data": {
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
}
```

## Error Response

```json
{
  "status": "error",
  "error": {
    "code": "CARD_NOT_PRESENT",
    "message": "Card is not present."
  }
}
```

The frontend shows the bridge error message to the operator and does not submit anything to the backend until the operator saves the reviewed payload.

## Backend Boundary

The backend remains the system of record. After a successful card read, the frontend submits the normalized payload to:

```http
PUT /api/verification/sessions/{transactionId}/dip-chip-payload
```

The backend validates, normalizes, masks responses, persists the identity entry, and records the audit event.
