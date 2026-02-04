# Contrat REST (API)

## Versioning

Toutes les routes publiques sont désormais versionnées sous `/api/v1`.
Les chemins exposés dans le code sont centralisés via `ApiPaths.API`.

## Pagination (cursor/limit)

La pagination est de type **cursor-based**.

### Conventions générales

- **limit** (integer, optionnel) : taille de page. Si omis ou à `0`, l’API applique sa limite par défaut.
- **cursor** : l’API utilise un couple de champs pour garantir un ordre stable.
- Le client renvoie le curseur reçu dans la réponse pour récupérer la page suivante.

### Ledger – historique de transactions

Endpoint : `POST /api/v1/ledger/transactions/search`

#### Requête

- `limit` : taille de page.
- `beforeCreatedAt` : horodatage du dernier élément de la page précédente.
- `beforeTransactionId` : identifiant du dernier élément de la page précédente.

Le couple `beforeCreatedAt` + `beforeTransactionId` sert de curseur.

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