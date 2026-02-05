# Contrat REST (API)

## Versioning

Toutes les routes publiques sont désormais versionnées sous `/api/v1`.
Les chemins exposés dans le code sont centralisés via `ApiPaths.API`.

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