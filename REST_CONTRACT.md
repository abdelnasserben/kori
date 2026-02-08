# KORI – REST API Contract

**Version : v1**
**Statut : Stable**

---

## 1. Introduction

KORI est une plateforme de paiement par carte permettant aux clients de payer chez des marchands,
avec des agents comme intermédiaires terrain pour l’enrôlement des cartes, les retraits marchands
et certaines opérations de gestion.

Cette API REST expose l’ensemble des opérations nécessaires à l’intégration de partenaires
(back-office, terminaux, systèmes tiers).

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

* Toutes les routes sont préfixées par `/api/v1`
* Toute modification incompatible entraînera une nouvelle version majeure (`/api/v2`)
* Les ajouts rétro-compatibles sont possibles au sein d’une même version

---

## 4. Authentification & autorisation

### Authentification

Toutes les routes (sauf exceptions documentées) nécessitent un **token JWT valide**.

### Actor Context (obligatoire)

Le token DOIT contenir un contexte acteur permettant d’identifier l’appelant.

Claims requis :

* `actor_type` ou `actorType`
  Valeurs possibles : `ADMIN`, `AGENT`, `MERCHANT`, `CLIENT`, `TERMINAL`

* `actor_id` ou `actorId`
  Identifiant unique de l’acteur
  À défaut, le claim standard `sub` est utilisé

### Règles d’accès

* Token absent, invalide ou expiré → **401 Unauthorized**
* Token valide mais droits insuffisants → **403 Forbidden**

---

## 5. Règles transverses

### 5.1 Idempotence

Certains endpoints d’écriture sont idempotents.

#### Header requis

```
Idempotency-Key: <string>
```

#### Comportement garanti

* Une même clé + même payload → même résultat
* Collision de clé avec payload différent → **409 Conflict**

Les endpoints idempotents sont explicitement indiqués.

---

### 5.2 Pagination

La pagination est de type **cursor-based**.

Conventions générales :

* `limit` (optionnel) : taille de page
* `cursor` (optionnel) : curseur opaque retourné par l’API
* `nextCursor` (réponse) : curseur de la page suivante ou `null`

---

### 5.3 Formats & conventions

* Dates/heures : ISO-8601 UTC
* Montants : nombres décimaux (precision financière)
* JSON UTF-8
* Champs inconnus ignorés par le serveur

---

## 6. Ressources & endpoints

### 6.1 Admins

#### POST `/api/v1/admins` (idempotent)

**Réponse**

```json
{ "adminId": "..." }
```

#### PATCH `/api/v1/admins/{adminId}/status`

**Requête**

```json
{ "targetStatus": "ACTIVE|SUSPENDED|CLOSED", "reason": "..." }
```

---

### 6.2 Agents

#### POST `/api/v1/agents` (idempotent)

```json
{ "agentId": "...", "agentCode": "..." }
```

#### PATCH `/api/v1/agents/{agentCode}/status`

```json
{ "targetStatus": "...", "reason": "..." }
```

---

### 6.3 Merchants

#### POST `/api/v1/merchants` (idempotent)

```json
{ "merchantId": "...", "code": "..." }
```

#### PATCH `/api/v1/merchants/{merchantCode}/status`

```json
{ "targetStatus": "...", "reason": "..." }
```

---

### 6.4 Terminals

#### POST `/api/v1/terminals` (idempotent)

```json
{ "merchantCode": "..." }
```

#### PATCH `/api/v1/terminals/{terminalId}/status`

```json
{ "targetStatus": "...", "reason": "..." }
```

---

### 6.5 Clients

#### PATCH `/api/v1/clients/{clientId}/status`

```json
{ "targetStatus": "...", "reason": "..." }
```

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
{ "targetStatus": "...", "reason": "..." }
```

#### POST `/api/v1/cards/{cardUid}/unblock`

```json
{ "reason": "..." }
```

---

### 6.7 Payments

#### POST `/api/v1/payments/card` (idempotent)

```json
{ "terminalUid": "...", "cardUid": "...", "pin": "1234", "amount": 0 }
```

#### POST `/api/v1/payments/merchant-withdraw` (idempotent)

```json
{ "merchantCode": "...", "agentCode": "...", "amount": 0 }
```

#### POST `/api/v1/payments/cash-in` (idempotent)

```json
{ "phoneNumber": "+269xxxxxxx", "amount": 0 }
```

#### POST `/api/v1/payments/agent-bank-deposits` (idempotent)

```json
{ "agentCode": "...", "amount": 0 }
```

#### POST `/api/v1/payments/reversals` (idempotent)

```json
{ "originalTransactionId": "..." }
```

---

### 6.8 Payouts

#### POST `/api/v1/payouts/requests` (idempotent)

```json
{ "agentCode": "..." }
```

#### POST `/api/v1/payouts/{payoutId}/complete`

→ `204 No Content`

#### POST `/api/v1/payouts/{payoutId}/fail`

```json
{ "reason": "..." }
```

---

### 6.9 Client refunds

#### POST `/api/v1/client-refunds/requests` (idempotent)

```json
{ "clientId": "..." }
```

#### POST `/api/v1/client-refunds/{refundId}/complete`

→ `204 No Content`

#### POST `/api/v1/client-refunds/{refundId}/fail`

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

## 7. Invariants métier majeurs

* Une carte est liée à un seul compte
* Une carte ne peut jamais être utilisée si elle a été précédemment utilisée ailleurs
* Un client possède exactement un compte
* Un client ne peut être clôturé que si son solde est égal à zéro
* Un remboursement client est toujours intégral
* Un seul remboursement en état `REQUESTED` est autorisé par client
* Toute opération financière est reversible via une transaction append-only

---

## 8. Modèle d’erreurs

### Format standard

```json
{
  "timestamp": "...",
  "code": "...",
  "message": "...",
  "details": {},
  "path": "..."
}
```

### Codes principaux

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

## 9. Support & évolution

* Ce contrat constitue la **référence unique** pour toute intégration
* Toute évolution incompatible fera l’objet d’une nouvelle version majeure
* Les ajouts non-cassants peuvent être introduits à tout moment
