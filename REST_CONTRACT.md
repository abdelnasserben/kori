# KORI – REST API Contract

Version: v1
Status: Stable

---

## 1. Overview

KORI is a card-based payment platform enabling:

* Customers to pay merchants using physical cards.
* Merchants to withdraw funds through field agents.
* Agents to enroll cards and process merchant withdrawals.
* Administrators to configure financial policies and supervise operations.

This document defines the official REST contract for all integrations (backoffice, mobile, terminals, third-party systems).

All routes are prefixed with:

```
/api/v1
```

---

## 2. Authentication & Security

### 2.1 Authentication

All endpoints require a valid JWT bearer token unless explicitly stated.

```
Authorization: Bearer <jwt>
```

### 2.2 Actor Context

The JWT must contain:

* `actor_type` (ADMIN, AGENT, MERCHANT, CLIENT, TERMINAL)
* `actor_id`
* Relevant permissions

Access control rules:

* Invalid or missing token → 401
* Insufficient permissions → 403

---

## 3. Cross-Cutting Rules

### 3.1 Correlation ID

Optional but recommended:

```
X-Correlation-Id: <uuid>
```

Returned in error responses.

---

### 3.2 Idempotency

Write operations marked idempotent require:

```
Idempotency-Key: <string>
```

Behavior:

* Same key + identical payload → same response
* Same key + different payload → 409
* Reserved key not completed → 409

---

### 3.3 Pagination

All list endpoints use cursor-based pagination:

Query:

```
limit
cursor
sort=<field>:asc|desc
```

Response:

```json
{
  "items": [],
  "page": {
    "nextCursor": "opaque",
    "hasMore": true
  }
}
```

---

# 4. Core Financial Operations

## 4.1 Card Payment

POST `/api/v1/payments/card` (idempotent)

Request:

```json
{
  "terminalUid": "string",
  "cardUid": "string",
  "pin": "1234",
  "amount": 1500.00
}
```

Response 201:

```json
{
  "transactionId": "string",
  "merchantCode": "string",
  "cardUid": "string",
  "amount": 1500.00,
  "fee": 30.00,
  "totalDebited": 1530.00
}
```

---

## 4.2 Merchant Withdrawal

POST `/api/v1/payments/merchant-withdraw` (idempotent)

---

## 4.3 Agent Cash-In

POST `/api/v1/payments/cash-in` (idempotent)

---

## 4.4 Transaction Reversal

POST `/api/v1/payments/reversals` (idempotent)

---

## 4.5 Agent Payout

POST `/api/v1/payouts/requests` (idempotent)

Lifecycle:

* REQUESTED
* COMPLETED
* FAILED

---

## 4.6 Client Refund

POST `/api/v1/client-refunds/requests` (idempotent)

---

# 5. Configuration APIs (Admin Only)

PATCH `/api/v1/config/fees`
PATCH `/api/v1/config/commissions`

All financial parameters are dynamic and configurable.

---

# 6. Actor Management

Create / update status for:

* `/admins`
* `/agents`
* `/merchants`
* `/clients`
* `/terminals`
* `/cards`

Status transitions validated server-side.

---

# 7. Self-Service APIs (/me)

Accessible based on actor_type:

## Client

* GET `/client/me/profile`
* GET `/client/me/balance`
* GET `/client/me/cards`
* GET `/client/me/transactions`
* GET `/client/me/transactions/{transactionId}`

## Merchant

* GET `/merchant/me/profile`
* GET `/merchant/me/balance`
* GET `/merchant/me/transactions`
* GET `/merchant/me/transactions/{transactionId}`
* GET `/merchant/me/terminals`

## Agent

* GET `/agent/me/summary`
* GET `/agent/me/transactions`
* GET `/agent/me/activities`
* GET `/agent/search`

---

# 8. Backoffice Read Namespace

Prefix:

```
/api/v1/backoffice
```

Admin-only access.

---

## 8.1 Transactions Listing

GET `/backoffice/transactions`

Supported filters:

* query
* type
* status
* actorType
* actorRef
* merchantCode
* agentCode
* terminalUid
* cardUid
* clientPhone
* from
* to
* min
* max

---

## 8.2 Transaction Details

GET `/backoffice/transactions/{transactionId}`

Response includes:

```json
{
  "transactionId": "...",
  "type": "...",
  "status": "...",
  "amount": 1500.00,
  "currency": "KMF",
  "merchantCode": "...",
  "agentCode": "...",
  "clientPhone": "...",
  "terminalUid": "...",
  "cardUid": "...",
  "originalTransactionId": "...",
  "payout": { ...},
  "clientRefund": { ... },
  "ledgerLines": [
    {
      "accountType": "...",
      "ownerRef": "...",
      "entryType": "DEBIT|CREDIT",
      "amount": 1500.00
    }
  ],
  "createdAt": "ISO-8601"
}
```

This endpoint reflects the authoritative ledger (append-only).

---

## 8.3 Audit Events

GET `/backoffice/audit-events`

Filters:

* action
* actorType
* actorId
* resourceType
* resourceId
* from
* to

---

## 8.4 Actors Listing

* `/backoffice/agents`
* `/backoffice/clients`
* `/backoffice/merchants`

---

# 9. Error Model

Standard structure:

```json
{
  "timestamp": "ISO-8601",
  "correlationId": "uuid",
  "errorId": "uuid",
  "code": "ERROR_CODE",
  "message": "Human readable",
  "details": {},
  "path": "/api/v1/..."
}
```

Common codes:

* INVALID_INPUT
* AUTHENTICATION_REQUIRED
* FORBIDDEN_OPERATION
* RESOURCE_NOT_FOUND
* IDEMPOTENCY_CONFLICT
* INSUFFICIENT_FUNDS
* TECHNICAL_FAILURE

---
