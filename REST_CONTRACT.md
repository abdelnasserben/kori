# Contrat REST (API)

## Versioning

Toutes les routes publiques sont désormais versionnées sous `/api/v1`.
Les chemins exposés dans le code sont centralisés via `ApiPaths.API`.

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
  - 
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

#### Réponse

- `nextBeforeCreatedAt`
- `nextBeforeTransactionId`

Si ces champs sont `null`, il n’y a plus de page suivante.

## Codes HTTP – endpoints mutatifs

### Créations (POST qui créent une ressource/transaction)

- **201 Created** avec un corps de réponse.

### Mises à jour (PATCH, POST d’action avec réponse)

- **200 OK** avec un corps de réponse décrivant le nouvel état.

### Actions sans corps de réponse

- **204 No Content** pour les actions idempotentes qui ne renvoient pas de payload.

## Config fees (`PATCH /api/v1/config/fees`)

Le payload supporte les flags de remboursement suivants :
- `cardPaymentFeeRefundable`
- `merchantWithdrawFeeRefundable`
- `cardEnrollmentPriceRefundable`

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