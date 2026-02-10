# KORI – REST API Contract

**Version : v1**
**Statut : Stable (MVP front-ready backoffice web + mobile)**

---

## 1. Introduction

KORI est une plateforme de paiement par carte permettant aux clients de payer chez des marchands,
avec des agents comme intermédiaires terrain pour l’enrôlement des cartes, les retraits marchands
et certaines opérations de gestion.

Cette API REST expose l’ensemble des opérations nécessaires à l’intégration de partenaires
(backoffice, terminaux, systèmes tiers).

Ce document décrit **le contrat fonctionnel et technique de l’API** :

* endpoints exposés
* règles de sécurité
* formats de requêtes et réponses
* invariants métier
* modèle d’erreurs

---

## 2. Concepts & acteurs

### Acteurs principaux

* **Admin**

  * Gère les statuts (clients, comptes, cartes)
  * Configure frais et commissions
  * Déclenche payouts, reversals et remboursements clients

* **Agent**

  * Enrôle et vend des cartes
  * Effectue les retraits marchands
  * Exécute les cash-in clients
  * Perçoit des commissions

* **Merchant**

  * Accepte les paiements par carte
  * Retire ses fonds auprès d’un agent

* **Client**

  * Identifié par un numéro de téléphone unique
  * Possède un compte et une ou plusieurs cartes
  * Effectue des paiements par carte

* **Terminal**

  * Identité machine
  * Initie exclusivement des paiements carte

---

## 3. Versioning & compatibilité

* Toutes les routes sont préfixées par `/api/v1`.
* Toute modification incompatible entraîne une nouvelle version majeure (`/api/v2`).
* Les ajouts rétro-compatibles sont possibles au sein d’une même version.

---

## 4. Authentification & autorisation

### 4.1 Authentification

Toutes les routes (sauf exceptions documentées) nécessitent un **Bearer token JWT valide**.

Header HTTP :

```http
Authorization: Bearer <jwt>
```

### 4.2 Actor Context (obligatoire)

Le token DOIT contenir un contexte acteur permettant d’identifier l’appelant.

Claims standards requis :

* `actor_type` (string, requis) : `ADMIN`, `AGENT`, `MERCHANT`, `CLIENT`, `TERMINAL`
* `actor_id` (string, requis) : identifiant unique de l’acteur
* `permissions` (array<string>, requis) : permissions métier normalisées (exemples : `transactions:read`, `audit:read`, `agents:write`)

Compatibilité :

* `actor_id` ou `actorId`
  Identifiant unique de l’acteur
  À défaut, le claim standard `sub` est utilisé
* Les variantes camelCase `actorType` et `actorId` sont tolérées temporairement.
* Si `actor_id`/`actorId` absent, `sub` peut être utilisé en fallback.

### 4.3 Règles d’accès

* Token absent, invalide ou expiré → **401 Unauthorized**
* Token valide mais droits insuffisants → **403 Forbidden**

### 4.4 Correlation ID

Chaque requête DOIT transmettre un identifiant de corrélation :

```http
X-Correlation-Id: <uuid-or-opaque-id>
```

Le serveur :

* propage cet identifiant dans les logs techniques,
* le retourne dans les réponses d’erreur via `correlationId`.

### 4.5 CORS & clients front

Conventions CORS pour endpoints backoffice :

* `Access-Control-Allow-Origin` : whitelist explicite des origines backoffice web (pas de `*` en production).
* `Access-Control-Allow-Headers` inclut au minimum :
  `Authorization`, `Content-Type`, `X-Correlation-Id`, `Idempotency-Key`.
* `Access-Control-Allow-Methods` inclut les méthodes utilisées par le contrat (`GET`, `POST`, `PATCH`, `OPTIONS`).

Conventions mobile :

* Authentification recommandée via OAuth2/OIDC **Authorization Code + PKCE**.
* Le mobile obtient un bearer token puis appelle l’API comme tout client OAuth protégé.

---

## 5. Règles transverses

### 5.1 Idempotence

Certains endpoints d’écriture sont idempotents.

Header requis :

```http
Idempotency-Key: <string>
```

Comportement garanti :

* Le serveur calcule un hash SHA-256 du payload JSON.
* Une même clé + même payload (hash identique) → même résultat (réponse mise en cache).
* Clé déjà réservée mais résultat pas encore disponible → **409 Conflict** (`IDEMPOTENCY_CONFLICT`).
* Collision de clé avec payload différent → **409 Conflict** (`IDEMPOTENCY_CONFLICT`).
* Les enregistrements d’idempotence expirent après 24h (TTL configurable côté serveur).

Les endpoints idempotents sont explicitement indiqués.

### 5.2 Pagination universelle (cursor-based)

Tous les endpoints de listing (actors, transactions, audit, etc.) utilisent le même standard.

#### Paramètres de requête

* `limit` (optionnel, integer) : nombre d’éléments demandés.
  * défaut recommandé : `20`
  * max recommandé : `100`
* `cursor` (optionnel, string opaque) : curseur de page fourni par l’API.
* `sort` (optionnel, string) : tri normalisé `<field>:<direction>` avec `direction ∈ {asc,desc}`.
  * Exemples : `createdAt:desc`, `updatedAt:asc`

#### Format de réponse

```json
{
  "items": [],
  "page": {
    "nextCursor": "opaque-cursor-or-null",
    "hasMore": true
  }
}
```

Règles :

* `items` est toujours présent (liste vide autorisée).
* `page.nextCursor` vaut `null` si aucune page suivante.
* `page.hasMore=false` implique `nextCursor=null`.

#### Exemple

**Request**

```http
GET /api/v1/backoffice/transactions?limit=20&cursor=eyJvZmZzZXQiOjIwfQ==&sort=createdAt:desc
Authorization: Bearer <jwt>
X-Correlation-Id: 1398dc2e-f2de-45aa-ae43-5c80cfb8ed33
```

**Response 200**

```json
{
  "items": [
    {
      "transactionId": "txn_20250101_0001",
      "type": "CARD_PAYMENT",
      "amount": 2500.00,
      "currency": "KMF",
      "status": "COMPLETED",
      "createdAt": "2025-01-01T09:10:11Z"
    }
  ],
  "page": {
    "nextCursor": "eyJvZmZzZXQiOjQwfQ==",
    "hasMore": true
  }
}
```

### 5.3 Formats & conventions

* Dates/heures : ISO-8601 UTC.
* Montants : nombres décimaux (precision financière).
* JSON UTF-8.
* Champs inconnus ignorés par le serveur.

---

## 6. Ressources & endpoints

> **Note** : les endpoints d’écriture existants conservent leur sémantique. Les ajouts “front-ready” portent sur la formalisation read/backoffice.

### 6.1 Admins

#### POST `/api/v1/admins` (idempotent)

**Response 201**

```json
{ "adminId": "..." }
```

#### PATCH `/api/v1/admins/{adminId}/status`

**Request**

```json
{ "targetStatus": "ACTIVE|SUSPENDED|CLOSED", "reason": "..." }
```

**Response 204**

---

### 6.2 Agents

#### POST `/api/v1/agents` (idempotent)

**Response 201**

```json
{ "agentId": "...", "agentCode": "..." }
```

#### PATCH `/api/v1/agents/{agentCode}/status`

**Request**

```json
{ "targetStatus": "...", "reason": "..." }
```

**Response 204**

---

### 6.3 Merchants

#### POST `/api/v1/merchants` (idempotent)

**Response 201**

```json
{ "merchantId": "...", "code": "..." }
```

#### PATCH `/api/v1/merchants/{merchantCode}/status`

**Request**

```json
{ "targetStatus": "...", "reason": "..." }
```

**Response 204**

---

### 6.4 Terminals

#### POST `/api/v1/terminals` (idempotent)

**Response 201**

```json
{ "merchantCode": "..." }
```

#### PATCH `/api/v1/terminals/{terminalId}/status`

**Request**

```json
{ "targetStatus": "...", "reason": "..." }
```

**Response 204**

---

### 6.5 Clients

#### PATCH `/api/v1/clients/{clientId}/status`

**Request**

```json
{ "targetStatus": "...", "reason": "..." }
```

**Response 204**

---

### 6.6 Cards

#### POST `/api/v1/cards/enroll` (idempotent)

```json
{ "phoneNumber": "+269xxxxxxx", "cardUid": "...", "pin": "1234", "agentCode": "..." }
```

#### POST `/api/v1/cards/add` (idempotent)

```json
{ "phoneNumber": "+269xxxxxxx", "cardUid": "...", "pin": "1234", "agentCode": "..." }
```

#### PATCH `/api/v1/cards/{cardUid}/status/admin`

```json
{ "targetStatus": "...", "reason": "..." }
```

#### PATCH `/api/v1/cards/{cardUid}/status/agent`

```json
{ "reason": "..." }
```

---

### 6.10 Ledger

#### GET `/api/v1/ledger/balance`

Query params : `accountType`, `ownerRef`

#### POST `/api/v1/ledger/transactions/search`

```json
{
  "accountType": "...",
  "ownerRef": "...",
  "beforeCreatedAt": "...",
  "beforeTransactionId": "...",
  "limit": 0
}
```

---

### 6.11 Backoffice read namespace (MVP front-ready)

Tous les endpoints de cette section sont en lecture (`GET`) et suivent :

* pagination universelle (`limit`, `cursor`, `sort`),
* filtres nommés et explicites,
* codes HTTP standards (`200`, `400`, `401`, `403`, `500`).

#### 6.11.1 Transactions listing

**GET** `/api/v1/backoffice/transactions`

Query params :

* pagination : `limit`, `cursor`, `sort=createdAt:desc|asc`
* filtres : `from`, `to`, `status`, `type`, `merchantCode`, `agentCode`, `clientId`, `minAmount`, `maxAmount`

**Response 200**

```json
{
  "items": [
    {
      "transactionId": "txn_001",
      "type": "CARD_PAYMENT",
      "status": "COMPLETED",
      "amount": 1500.00,
      "currency": "KMF",
      "merchantCode": "MRC123",
      "agentCode": null,
      "clientId": "cli_001",
      "createdAt": "2025-01-01T08:00:00Z"
    }
  ],
  "page": {
    "nextCursor": "eyJvZmZzZXQiOjIwfQ==",
    "hasMore": true
  }
}
```

#### 6.11.2 Audit listing

**GET** `/api/v1/backoffice/audit/events`

Query params :

* pagination : `limit`, `cursor`, `sort=occurredAt:desc|asc`
* filtres : `actorType`, `actorId`, `action`, `resourceType`, `resourceId`, `from`, `to`

**Response 200**

```json
{
  "items": [
    {
      "eventId": "aud_001",
      "occurredAt": "2025-01-01T10:00:00Z",
      "actorType": "ADMIN",
      "actorId": "adm_001",
      "action": "MERCHANT_STATUS_CHANGED",
      "resourceType": "MERCHANT",
      "resourceId": "MRC123",
      "metadata": { "from": "ACTIVE", "to": "SUSPENDED" }
    }
  ],
  "page": {
    "nextCursor": null,
    "hasMore": false
  }
}
```

#### 6.11.3 Agents listing

**GET** `/api/v1/backoffice/actors/agents`

Query params :

* pagination : `limit`, `cursor`, `sort=createdAt:desc|asc`
* filtres : `status`, `q` (search code/nom/téléphone), `region`

**Response 200** : format `items[] + page`.

#### 6.11.4 Merchants listing

**GET** `/api/v1/backoffice/actors/merchants`

Query params :

* pagination : `limit`, `cursor`, `sort=createdAt:desc|asc`
* filtres : `status`, `q` (search code/nom), `category`

**Response 200** : format `items[] + page`.

#### 6.11.5 Clients listing

**GET** `/api/v1/backoffice/actors/clients`

Query params :

* pagination : `limit`, `cursor`, `sort=createdAt:desc|asc`
* filtres : `status`, `q` (search téléphone/clientId), `kycStatus`

**Response 200** : format `items[] + page`.

---

## 7. Invariants métier majeurs

* Une carte est liée à un seul compte.
* Une carte ne peut jamais être utilisée si elle a été précédemment utilisée ailleurs.
* Un client possède exactement un compte.
* Un client ne peut être clôturé que si son solde est égal à zéro.
* Un remboursement client est toujours intégral.
* Un seul remboursement en état `REQUESTED` est autorisé par client.
* Toute opération financière est reversible via une transaction append-only.

---

## 8. Modèle d’erreurs

### 8.1 Format standard enrichi

```json
{
  "timestamp": "2025-01-01T10:00:00Z",
  "correlationId": "1398dc2e-f2de-45aa-ae43-5c80cfb8ed33",
  "errorId": "5f8e80dc-4ff1-4da3-bf7f-0e4b5c3d67a1",
  "code": "INVALID_INPUT",
  "message": "Le paramètre limit doit être compris entre 1 et 100.",
  "details": {
    "field": "limit",
    "rejectedValue": 1000
  },
  "path": "/api/v1/backoffice/transactions"
}
```

Contraintes :

* `correlationId` : miroir du header `X-Correlation-Id`.
* `errorId` : UUID interne optionnel (trace support/ops).
* `message` : lisible côté front, non technique.
* `details` : objet libre pour erreurs de validation et contexte métier.

### 8.2 Exemples d’erreurs

#### 400 INVALID_INPUT

```json
{
  "timestamp": "2025-01-01T10:00:00Z",
  "correlationId": "1398dc2e-f2de-45aa-ae43-5c80cfb8ed33",
  "errorId": "c1f4f66d-6cb2-4a17-a33d-f7e0baf23d66",
  "code": "INVALID_INPUT",
  "message": "Le format de sort est invalide. Utiliser <field>:<asc|desc>.",
  "details": {
    "field": "sort",
    "rejectedValue": "createdAt:descending"
  },
  "path": "/api/v1/backoffice/transactions"
}
```

#### 401 AUTHENTICATION_REQUIRED

```json
{
  "timestamp": "2025-01-01T10:01:00Z",
  "correlationId": "1398dc2e-f2de-45aa-ae43-5c80cfb8ed33",
  "errorId": "8f36f48a-6eeb-4db6-bdf8-9f4fef2fd9f2",
  "code": "AUTHENTICATION_REQUIRED",
  "message": "Authentification requise.",
  "details": {},
  "path": "/api/v1/backoffice/audit/events"
}
```

#### 403 FORBIDDEN_OPERATION

```json
{
  "timestamp": "2025-01-01T10:02:00Z",
  "correlationId": "1398dc2e-f2de-45aa-ae43-5c80cfb8ed33",
  "errorId": "e944a4ca-4e6f-4b67-a88a-08eb22cbf1ba",
  "code": "FORBIDDEN_OPERATION",
  "message": "Permissions insuffisantes pour consulter les événements d'audit.",
  "details": {
    "requiredPermission": "audit:read"
  },
  "path": "/api/v1/backoffice/audit/events"
}
```

### 8.3 Codes principaux

#### Sécurité

* `401 AUTHENTICATION_REQUIRED`
* `403 FORBIDDEN_OPERATION`

#### Métier

* `400 INVALID_INPUT`
* `404 RESOURCE_NOT_FOUND`
* `409 IDEMPOTENCY_CONFLICT`
* `409 INSUFFICIENT_FUNDS`
* `409 BALANCE_MUST_BE_ZERO`

#### Technique

* `500 TECHNICAL_FAILURE`

---

## 9. Endpoints backoffice read standardisés (récapitulatif)

* `GET /api/v1/backoffice/transactions`
  * params : `limit`, `cursor`, `sort`, `from`, `to`, `status`, `type`, `merchantCode`, `agentCode`, `clientId`, `minAmount`, `maxAmount`
  * codes : `200`, `400`, `401`, `403`, `500`
* `GET /api/v1/backoffice/audit/events`
  * params : `limit`, `cursor`, `sort`, `actorType`, `actorId`, `action`, `resourceType`, `resourceId`, `from`, `to`
  * codes : `200`, `400`, `401`, `403`, `500`
* `GET /api/v1/backoffice/actors/agents`
  * params : `limit`, `cursor`, `sort`, `status`, `q`, `region`
  * codes : `200`, `400`, `401`, `403`, `500`
* `GET /api/v1/backoffice/actors/merchants`
  * params : `limit`, `cursor`, `sort`, `status`, `q`, `category`
  * codes : `200`, `400`, `401`, `403`, `500`
* `GET /api/v1/backoffice/actors/clients`
  * params : `limit`, `cursor`, `sort`, `status`, `q`, `kycStatus`
  * codes : `200`, `400`, `401`, `403`, `500`

---

## 10. Support & évolution

* Ce contrat constitue la **référence unique** pour toute intégration.
* Toute évolution incompatible fera l’objet d’une nouvelle version majeure.
* Les ajouts non-cassants peuvent être introduits à tout moment.