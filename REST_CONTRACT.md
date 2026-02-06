# Contrat REST (API)

## Versioning

Toutes les routes publiques sont désormais versionnées sous `/api/v1`.
Les chemins exposés dans le code sont centralisés via `ApiPaths.API`.

## Authentification & Actor context

L'API est sécurisée via JWT (resource server). Au-delà des rôles, un **actor context** est obligatoire
dans les claims du JWT, sinon la requête est rejetée.

Claims attendus (au moins) :
- `actor_type` (ou `actorType`) : `ADMIN`, `AGENT`, `MERCHANT`, `CLIENT`, `TERMINAL`
- `actor_id` (ou `actorId`, à défaut `sub`) : identifiant de l'acteur

Si le token est présent mais ces claims manquent ou sont invalides, la réponse est **403** avec le code
`AUTHENTICATION_REQUIRED` (cf. Matrice d'erreurs).

## Idempotency

Les endpoints annotés **idempotent** exigent l'en-tête `Idempotency-Key`. Le serveur calcule
un hash du payload et refuse les collisions de clé avec payload différent.

Endpoints idempotents (extraits du code) :
- `POST /api/v1/admins`
- `POST /api/v1/agents`
- `POST /api/v1/merchants`
- `POST /api/v1/terminals`
- `POST /api/v1/cards/enroll`
- `POST /api/v1/payments/card`
- `POST /api/v1/payments/merchant-withdraw`
- `POST /api/v1/payments/cash-in`
- `POST /api/v1/payments/agent-bank-deposits`
- `POST /api/v1/payments/reversals`
- `POST /api/v1/payouts/requests`
- `POST /api/v1/client-refunds/requests`

## Sécurité des endpoints (étape 1)

Politique globale:
- **deny-by-default** pour toute route non mappée explicitement.
- **whitelist ouverte** sans token pour:
  - OpenAPI/Swagger (`/api-docs/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`)
  - Health check (`/actuator/health/**`)

Mapping endpoint -> rôle attendu (scan de tous les contrôleurs REST):

- **Admin uniquement**
  - `POST /api/v1/admins`
  - `PATCH /api/v1/admins/{adminId}/status`
  - `POST /api/v1/agents`
  - `PATCH /api/v1/agents/{agentCode}/status`
  - `POST /api/v1/merchants`
  - `PATCH /api/v1/merchants/{merchantCode}/status`
  - `POST /api/v1/terminals`
  - `PATCH /api/v1/terminals/{terminalId}/status`
  - `PATCH /api/v1/clients/{clientId}/status`
  - `PATCH /api/v1/account-profiles/status`
  - `PATCH /api/v1/config/fees`
  - `PATCH /api/v1/config/commissions`
  - `POST /api/v1/client-refunds/requests`
  - `POST /api/v1/client-refunds/{refundId}/complete`
  - `POST /api/v1/client-refunds/{refundId}/fail`
  - `POST /api/v1/payments/agent-bank-deposits`
  - `POST /api/v1/payments/reversals`
  - `POST /api/v1/payouts/requests`
  - `POST /api/v1/payouts/{payoutId}/complete`
  - `POST /api/v1/payouts/{payoutId}/fail`
  - `PATCH /api/v1/cards/{cardUid}/status/admin`
  - `POST /api/v1/cards/{cardUid}/unblock`

- **Agent uniquement**
  - `POST /api/v1/cards/enroll`
  - `PATCH /api/v1/cards/{cardUid}/status/agent`
  - `POST /api/v1/payments/merchant-withdraw`
  - `POST /api/v1/payments/cash-in`

- **Terminal uniquement**
  - `POST /api/v1/payments/card`

- **Lecture ledger (ADMIN, AGENT, MERCHANT, CLIENT)**
  - `GET /api/v1/ledger/balance`
  - `POST /api/v1/ledger/transactions/search`


## Modèles de requêtes / réponses (contrat effectif)

### Admins
- `POST /api/v1/admins` (idempotent)
  - **Réponse**: `{ "adminId": "..." }`
- `PATCH /api/v1/admins/{adminId}/status`
  - **Requête**: `{ "targetStatus": "ACTIVE|SUSPENDED|CLOSED", "reason": "..."? }`
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`

### Agents
- `POST /api/v1/agents` (idempotent)
  - **Réponse**: `{ "agentId": "...", "agentCode": "..." }`
- `PATCH /api/v1/agents/{agentCode}/status`
  - **Requête**: `{ "targetStatus": "...", "reason": "..."? }`
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`

### Merchants
- `POST /api/v1/merchants` (idempotent)
  - **Réponse**: `{ "merchantId": "...", "code": "..." }`
- `PATCH /api/v1/merchants/{merchantCode}/status`
  - **Requête**: `{ "targetStatus": "...", "reason": "..."? }`
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`

### Terminals
- `POST /api/v1/terminals` (idempotent)
  - **Requête**: `{ "merchantCode": "..." }`
  - **Réponse**: `{ "terminalId": "...", "merchantCode": "..." }`
- `PATCH /api/v1/terminals/{terminalId}/status`
  - **Requête**: `{ "targetStatus": "...", "reason": "..."? }`
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`

### Clients
- `PATCH /api/v1/clients/{clientId}/status`
  - **Requête**: `{ "targetStatus": "...", "reason": "..."? }`
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`

### Account profiles
- `PATCH /api/v1/account-profiles/status`
  - **Requête**: `{ "accountType": "...", "ownerRef": "...", "targetStatus": "...", "reason": "..."? }`
  - **Réponse**: `{ "accountType": "...", "ownerRef": "...", "previousStatus": "...", "newStatus": "..." }`

### Cards
- `POST /api/v1/cards/enroll` (idempotent)
  - **Requête**: `{ "phoneNumber": "+269xxxxxxx", "cardUid": "...", "pin": "1234", "agentCode": "..." }`
  - **Réponse**: `{ "transactionId": "...", "clientPhoneNumber": "...", "cardUid": "...", "cardPrice": 0, "agentCommission": 0, "clientCreated": true|false, "clientAccountProfileCreated": true|false }`
- `PATCH /api/v1/cards/{cardUid}/status/admin`
  - **Requête**: `{ "targetStatus": "...", "reason": "..."? }`
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`
- `POST /api/v1/cards/{cardUid}/unblock`
  - **Requête**: `{ "reason": "..."? }` (optionnelle)
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`
- `PATCH /api/v1/cards/{cardUid}/status/agent`
  - **Requête**: `{ "agentCode": "...", "targetStatus": "...", "reason": "..."? }`
  - **Réponse**: `{ "subjectId": "...", "previousStatus": "...", "newStatus": "..." }`

### Payments
- `POST /api/v1/payments/card` (idempotent)
  - **Requête**: `{ "terminalUid": "...", "cardUid": "...", "pin": "1234", "amount": 0 }`
  - **Réponse**: `{ "transactionId": "...", "merchantCode": "...", "cardUid": "...", "amount": 0, "fee": 0, "totalDebited": 0 }`
- `POST /api/v1/payments/merchant-withdraw` (idempotent)
  - **Requête**: `{ "merchantCode": "...", "agentCode": "...", "amount": 0 }`
  - **Réponse**: `{ "transactionId": "...", "merchantCode": "...", "agentCode": "...", "amount": 0, "fee": 0, "commission": 0, "totalDebitedMerchant": 0 }`
- `POST /api/v1/payments/cash-in` (idempotent)
  - **Requête**: `{ "phoneNumber": "+269xxxxxxx", "amount": 0 }`
  - **Réponse**: `{ "transactionId": "...", "agentId": "...", "clientId": "...", "clientPhoneNumber": "...", "amount": 0 }`
- `POST /api/v1/payments/agent-bank-deposits` (idempotent)
  - **Requête**: `{ "agentCode": "...", "amount": 0 }`
  - **Réponse**: `{ "transactionId": "...", "agentCode": "...", "amount": 0 }`
- `POST /api/v1/payments/reversals` (idempotent)
  - **Requête**: `{ "originalTransactionId": "..." }`
  - **Réponse**: `{ "transactionId": "...", "originalTransactionId": "..." }`

### Payouts
- `POST /api/v1/payouts/requests` (idempotent)
  - **Requête**: `{ "agentCode": "..." }`
  - **Réponse**: `{ "transactionId": "...", "payoutId": "...", "agentCode": "...", "amount": 0, "status": "REQUESTED|COMPLETED|FAILED" }`
- `POST /api/v1/payouts/{payoutId}/complete`
  - **Réponse**: `204 No Content`
- `POST /api/v1/payouts/{payoutId}/fail`
  - **Requête**: `{ "reason": "..." }`
  - **Réponse**: `204 No Content`

### Client refunds
- `POST /api/v1/client-refunds/requests` (idempotent)
  - **Requête**: `{ "clientId": "..." }`
  - **Réponse**: `{ "transactionId": "...", "refundId": "...", "clientId": "...", "amount": 0, "status": "REQUESTED|COMPLETED|FAILED" }`
- `POST /api/v1/client-refunds/{refundId}/complete`
  - **Réponse**: `204 No Content`
- `POST /api/v1/client-refunds/{refundId}/fail`
  - **Requête**: `{ "reason": "..." }`
  - **Réponse**: `204 No Content`

### Ledger
- `GET /api/v1/ledger/balance`
  - **Query params**: `accountType`?, `ownerRef`?
  - **Réponse**: `{ "accountType": "...", "ownerRef": "...", "balance": 0 }`
- `POST /api/v1/ledger/transactions/search`
  - **Requête**: `{ "accountType": "...", "ownerRef": "...", "transactionType": "...", "from": "...", "to": "...", "beforeCreatedAt": "...", "beforeTransactionId": "...", "minAmount": 0, "maxAmount": 0, "view": "...", "limit": 0 }`
  - **Réponse**: `{ "ledgerScope": { "accountType": "...", "ownerRef": "..." }, "items": [...], "nextBeforeCreatedAt": "...", "nextBeforeTransactionId": "..." }`

## Pagination (cursor/limit)

La pagination est de type **cursor-based** et standardisée pour toutes les futures listes (clients,
marchands, etc.), afin d’assurer une compatibilité long terme avec le ledger.

### Conventions générales (standard)

- **limit** (integer, optionnel) : taille de page. Si omis ou à `0`, l’API applique sa limite par défaut.
- **cursor** (string, optionnel) : curseur opaque, renvoyé par l’API et réutilisé tel quel par le client.
- **nextCursor** (string, réponse) : curseur de la page suivante. S’il est `null`, il n’y a plus de page.

Cette convention s’appliquera aux nouvelles listes (clients, marchands, etc.).

### Ledger – historique de transactions

Endpoint : `POST /api/v1/ledger/transactions/search`

#### Requête

- `limit` : taille de page.
- `beforeCreatedAt` : horodatage du dernier élément de la page précédente.
- `beforeTransactionId` : identifiant du dernier élément de la page précédente.

Le couple `beforeCreatedAt` + `beforeTransactionId` sert de curseur stable et restera la base
des listes qui doivent s’aligner sur la compatibilité ledger.
@@ -109,71 +240,78 @@ Le payload supporte les flags de remboursement suivants :
Compatibilité descendante : si ces champs sont omis, la valeur appliquée est explicitement `false` (default SAFE).
## Ledger account model (Slice 5 cleanup)

- Le compte `AGENT` a été retiré du modèle actif.
- Les commissions agent utilisent `AGENT_WALLET`.
- Les flux cash agent utilisent `AGENT_CASH_CLEARING`.
- Lors de la création d'un agent, les deux profils de compte (`AGENT_WALLET` et `AGENT_CASH_CLEARING`) sont provisionnés.
## Agent bank deposit receipt (`POST /api/v1/payments/agent-bank-deposits`)

Use case admin-only pour constater un dépôt bancaire effectué par un agent.

- Requête : `agentCode`, `amount` (+ headers idempotency standards).
- Réponse : `transactionId`, `agentCode`, `amount`.
- Écriture ledger : **Debit `PLATFORM_BANK` / Credit `AGENT_CASH_CLEARING`**.
- Invariant métier : la position cash agent (`AGENT_CASH_CLEARING`) est créditée, ce qui réduit son exposition cash.
- Traçabilité : audit `AGENT_BANK_DEPOSIT_RECEIPT`.

## Close client account (admin only)

Le use case de clôture client passe par la mise à jour de statut client vers `CLOSED`.

- Précondition : `CLIENT_WALLET` doit avoir un solde net égal à `0`.
- Si le solde est non nul : la requête est refusée, sans changement d'état du client, des cartes ou du compte.
- Si le client est déjà `CLOSED` : opération idempotente (no-op).
- Aucune écriture ledger additionnelle n'est créée par la clôture (seulement changement d'état + audit/event existants).

⚠️ Implémentation actuelle : le refus "solde non nul" est levé via `IllegalStateException`,
ce qui produit un **500** côté API (non mappé en 4xx). Ce point ne reflète pas un contrat REST stable.

## Client refunds (`/api/v1/client-refunds`)

Flux admin-only pour rembourser intégralement le solde d'un client avant clôture.

- `POST /requests` : crée un remboursement `REQUESTED` pour le montant exact du wallet client.
  - Écriture ledger : **Debit `CLIENT` / Credit `PLATFORM_CLIENT_REFUND_CLEARING`**.
- `POST /{refundId}/complete` : marque `COMPLETED` lorsque le virement banque est confirmé.
  - Écriture ledger : **Debit `PLATFORM_CLIENT_REFUND_CLEARING` / Credit `PLATFORM_BANK`**.
- `POST /{refundId}/fail` : marque `FAILED` et restitue le montant au client.
  - Écriture ledger : **Debit `PLATFORM_CLIENT_REFUND_CLEARING` / Credit `CLIENT`**.

Contraintes:
- montant de remboursement = solde intégral du client
- un seul remboursement `REQUESTED` à la fois par client
## Matrice d'erreurs REST (étape 3)

Format JSON d'erreur unifié (métier + sécurité):
- `timestamp`: horodatage UTC
- `code`: code applicatif stable
- `message`: message public
- `details`: objet JSON (vide `{}` si rien à exposer)
- `path`: chemin HTTP demandé

### Sécurité
- **401 Unauthorized**
  - Code: `AUTHENTICATION_REQUIRED`
  - Message: `Authentication required`
  - Cas: token absent/invalide/expiré
- **403 Forbidden**
  - Code: `FORBIDDEN_OPERATION`
  - Message: `Forbidden operation`
  - Cas: token valide mais rôle insuffisant

### Métier/validation
- **400 Bad Request**
  - Code: `INVALID_INPUT` (ou code validation dédié)
  - Cas: validation Bean Validation / contraintes d'entrée
- **404 Not Found**
  - Code: `RESOURCE_NOT_FOUND`
  - Cas: entité introuvable
- **409 Conflict**
  - Code: `IDEMPOTENCY_CONFLICT`, `INSUFFICIENT_FUNDS`, ou autre code métier de conflit
  - Cas: invariants métier/idempotence

### Technique
- **5xx Server Error**
  - Code: `TECHNICAL_FAILURE`
  - Message: `Unexpected error`
  - `details`: `{}` (aucune fuite d'information technique)