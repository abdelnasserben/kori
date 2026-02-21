# KORI REST API – OFFICIAL CONTRACT (v1)

Ce document décrit l’ensemble des endpoints exposés par l’API `v1`, les rôles autorisés, les règles de filtrage/pagination, les formats standard et la structure des erreurs.

---

# 1) Base API

* **Base URL** : `/api/v1`
* **Format** : `application/json`
* **Authentification** : Bearer JWT
* **Fuseau horaire** : UTC
* **Versioning** : URI-based (`/v1`)

Endpoints publics :

* `/v3/api-docs/**`
* `/swagger-ui/**`
* `/actuator/health/**`

---

# 2) Authentification & Rôles

## Rôles applicatifs

* `ADMIN`
* `AGENT`
* `MERCHANT`
* `CLIENT`
* `TERMINAL`

## JWT Requirements

Le token JWT doit contenir :

* `sub`
* `roles`
* `actorType`
* `actorRef`

Les endpoints `/me` utilisent automatiquement `actorRef` du token.

---

# 3) Standards Globaux

---

## 3.1 Format des dates

* ISO-8601
* UTC
* Exemple :

```
2026-02-20T10:15:30Z
```

---

## 3.2 Format des montants

* Type : decimal
* Devise : `KMF`
* Toujours positifs
* Le sens débit/crédit dépend du contexte

---

## 3.3 Format des réponses

### 🔹 Réponse simple

```json
{
  ...objet...
}
```

### 🔹 Réponse paginée

```json
{
  "items": [ ... ],
  "page": {
    "nextCursor": "opaque-string-or-null",
    "hasMore": true
  }
}
```

* `nextCursor` doit être réutilisé tel quel.
* `hasMore` indique s’il reste des éléments.

---

## 3.4 Idempotence

Pour les opérations financières `POST` :

Header requis :

```
Idempotency-Key: <uuid>
```

Règles :

* Même clé + même body → même résultat
* Même clé + body différent → `409 Conflict`

---

## 3.5 Tri

Paramètre :

```
sort=createdAt
sort=-createdAt
```

* Défaut : `-createdAt`

---

## 3.6 Pagination

* `limit` : borné côté serveur
* `cursor` : opaque, non modifiable

---

# 4) Écriture (Write Side)

---

## 4.1 Administration (ADMIN)

| Méthode | Endpoint                           | Description                  |
| ------- | ---------------------------------- | ---------------------------- |
| POST    | `/admins`                          | Créer admin                  |
| PATCH   | `/admins/{adminUsername}/status`   | Changer statut admin         |
| POST    | `/agents`                          | Créer agent                  |
| PATCH   | `/agents/{agentCode}/status`       | Changer statut agent         |
| POST    | `/merchants`                       | Créer marchand               |
| PATCH   | `/merchants/{merchantCode}/status` | Changer statut marchand      |
| POST    | `/terminals`                       | Créer terminal               |
| PATCH   | `/terminals/{terminalUid}/status`  | Changer statut terminal      |
| PATCH   | `/clients/{clientCode}/status`     | Changer statut client        |
| PATCH   | `/account-profiles/status`         | Changer statut profil compte |

---

## 4.2 Configuration (ADMIN)

| Méthode | Endpoint              | Description                     |
| ------- | --------------------- | ------------------------------- |
| GET     | `/config/fees`        | Lire la configuration des frais |
| PATCH   | `/config/fees`        | Mise à jour des frais           |
| GET     | `/config/commissions` | Lire la configuration commissions |
| PATCH   | `/config/commissions` | Mise à jour commissions         |
| GET     | `/config/platform`    | Lire les paramètres plateforme  |
| PATCH   | `/config/platform`    | Paramètres plateforme           |

---

## 4.3 Cartes

| Méthode | Endpoint                        | Rôle  |
| ------- | ------------------------------- | ----- |
| POST    | `/cards/enroll`                 | AGENT |
| POST    | `/cards/add`                    | AGENT |
| PATCH   | `/cards/{cardUid}/status/agent` | AGENT |
| PATCH   | `/cards/{cardUid}/status/admin` | ADMIN |
| POST    | `/cards/{cardUid}/unblock`      | ADMIN |

---

## 4.4 Opérations financières

| Endpoint                             | Rôle     |
| ------------------------------------ | -------- |
| POST `/payments/card`                | TERMINAL |
| POST `/payments/merchant-withdraw`   | AGENT    |
| POST `/payments/cash-in`             | AGENT    |
| POST `/payments/client-transfer`     | CLIENT   |
| POST `/payments/merchant-transfer`   | MERCHANT |
| POST `/payments/agent-bank-deposits` | ADMIN    |
| POST `/payments/reversals`           | ADMIN    |

---

## Exemple – Transfert P2P

### Request

```http
POST /payments/client-transfer
Authorization: Bearer <token>
Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000
```

```json
{
  "recipientPhoneNumber": "+2697734567",
  "amount": 1000
}
```

### Response

```json
{
  "transactionId": "TX-12345",
  "senderClientCode": "C-001",
  "recipientClientCode": "C-002",
  "recipientPhoneNumber": "+2697734567",
  "amount": 1000,
  "fee": 10,
  "totalDebited": 1010
}
```

### Exemple – Transfert M2M

### Request

```http
POST /payments/merchant-transfer
Authorization: Bearer <token>
Idempotency-Key: 123e4567-e89b-12d3-a456-426614174001
```

```json
{
  "recipientMerchantCode": "M-002",
  "amount": 5000
}
```

### Response

```json
{
  "transactionId": "TX-67890",
  "senderMerchantCode": "M-001",
  "recipientMerchantCode": "M-002",
  "amount": 5000,
  "fee": 25,
  "totalDebited": 5025
}
```


---

# 5) Lecture – Ledger (ADMIN)

| Méthode | Endpoint                      |
| ------- | ----------------------------- |
| GET     | `/ledger/balance`             |
| POST    | `/ledger/transactions/search` |

Filtres principaux :

* `accountType`
* `ownerRef`
* `transactionType`
* `from`
* `to`
* `minAmount`
* `maxAmount`
* `limit`

---

# 6) Self-Service

---

## CLIENT

| Endpoint                                       |
| ---------------------------------------------- |
| GET `/client/me/home`                          |
| GET `/client/me/profile`                       |
| GET `/client/me/balance`                       |
| GET `/client/me/cards`                         |
| GET `/client/me/transactions`                  |
| GET `/client/me/transactions/{transactionRef}` |

Filtres transactions :

* `type`
* `status`
* `from`
* `to`
* `min`
* `max`
* `limit`
* `cursor`
* `sort`

---

## MERCHANT

| Endpoint                                         |
| ------------------------------------------------ |
| GET `/merchant/me/profile`                       |
| GET `/merchant/me/balance`                       |
| GET `/merchant/me/transactions`                  |
| GET `/merchant/me/transactions/{transactionRef}` |
| GET `/merchant/me/terminals`                     |
| GET `/merchant/me/terminals/{terminalUid}`       |

---

## AGENT

| Endpoint                     |
| ---------------------------- |
| GET `/agent/me/summary`      |
| GET `/agent/me/transactions` |
| GET `/agent/me/activities`   |
| GET `/agent/search`          |

---

## TERMINAL

| Endpoint                  |
| ------------------------- |
| GET `/terminal/me/status` |
| GET `/terminal/me/config` |
| GET `/terminal/me/health` |

---

# 7) Backoffice (ADMIN)

---

## Transactions

GET `/backoffice/transactions`

Filtres :

* `query` (recherche libre sur transactionRef, merchantCode, agentCode, clientCode)
* `type`
* `status`
* `actorType`
* `actorRef`
* `terminalUid`
* `cardUid`
* `merchantCode`
* `agentCode`
* `clientPhone`
* `from`
* `to`
* `min`
* `max`
* `limit`
* `cursor`
* `sort`

---

## Audit

GET `/backoffice/audit-events`

Filtres :

* `action`
* `actorType`
* `actorRef`
* `resourceType`
* `resourceRef`
* `from`
* `to`

---

## Acteurs

GET :

* `/backoffice/agents`
* `/backoffice/clients`
* `/backoffice/merchants`
* `/backoffice/actors/{actorType}/{actorRef}`

---

## Lookup

GET `/backoffice/lookups`

* `q`
* `type`
* `limit`

---

# 8) Valeurs Enum Principales

## Transaction Types

* `CARD_PAYMENT`
* `CASH_IN`
* `CLIENT_TRANSFER`
* `MERCHANT_TRANSFER`
* `MERCHANT_WITHDRAW`
* `REVERSAL`
* `AGENT_BANK_DEPOSIT`

## Transaction Status

* `PENDING`
* `COMPLETED`
* `FAILED`
* `REVERSED`

## Actor Status

* `ACTIVE`
* `INACTIVE`
* `SUSPENDED`

---

# 9) Format d’erreur standard

Toutes les erreurs retournent :

```json
{
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Client balance is insufficient",
  "correlationId": "uuid",
  "timestamp": "2026-02-20T10:15:30Z"
}
```

Codes possibles :

* `INSUFFICIENT_FUNDS`
* `DAILY_LIMIT_EXCEEDED`
* `MAX_TRANSACTION_EXCEEDED`
* `INVALID_STATUS`
* `IDEMPOTENCY_CONFLICT`
* `UNAUTHORIZED`
* `FORBIDDEN`

---

# 10) Référence officielle

L’OpenAPI généré est la source technique de vérité :


Ce document est la référence fonctionnelle destinée aux équipes :

* Mobile
* Web
* Backoffice
* QA
* Intégration
