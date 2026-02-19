# KORI REST Contract (version exclusive)

Ce document remplace totalement l'ancien contrat et décrit **tous les endpoints exposés** par l'API `v1`, avec rôles, paramètres, tri/pagination/filtrage.

---

## 1) Base API

- Base URL: `/api/v1`
- Format: `application/json`
- Auth: Bearer JWT (sauf Swagger/health)
- Idempotence: pour les opérations financières `POST`, header recommandé `Idempotency-Key`

---

## 2) Authentification & rôles

Rôles applicatifs:
- `ADMIN`
- `AGENT`
- `MERCHANT`
- `CLIENT`
- `TERMINAL`

Endpoints publics (sans JWT):
- `/api-docs/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/actuator/health/**`

---

## 3) Écriture (command side)

### 3.1 Administration

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| POST | `/admins` | ADMIN | Créer un admin |
| PATCH | `/admins/{adminUsername}/status` | ADMIN | Changer statut admin |
| POST | `/agents` | ADMIN | Créer un agent |
| PATCH | `/agents/{agentCode}/status` | ADMIN | Changer statut agent |
| POST | `/merchants` | ADMIN | Créer un marchand |
| PATCH | `/merchants/{merchantCode}/status` | ADMIN | Changer statut marchand |
| POST | `/terminals` | ADMIN | Créer un terminal |
| PATCH | `/terminals/{terminalUid}/status` | ADMIN | Changer statut terminal |
| PATCH | `/clients/{clientCode}/status` | ADMIN | Changer statut client |
| PATCH | `/account-profiles/status` | ADMIN | Changer statut profil compte |

---

### 3.2 Config

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| PATCH | `/config/fees` | ADMIN | Mise à jour des frais |
| PATCH | `/config/commissions` | ADMIN | Mise à jour des commissions |
| PATCH | `/config/platform` | ADMIN | Mise à jour paramètres plateforme |

---

### 3.3 Cartes

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| POST | `/cards/enroll` | AGENT | Enrôler une carte |
| POST | `/cards/add` | AGENT | Ajouter carte à client existant |
| PATCH | `/cards/{cardUid}/status/agent` | AGENT | Changer statut carte (agent) |
| PATCH | `/cards/{cardUid}/status/admin` | ADMIN | Changer statut carte (admin) |
| POST | `/cards/{cardUid}/unblock` | ADMIN | Débloquer carte |

---

### 3.4 Paiements / opérations financières

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| POST | `/payments/card` | TERMINAL | Paiement carte client -> marchand |
| POST | `/payments/merchant-withdraw` | AGENT | Retrait marchand via agent |
| POST | `/payments/cash-in` | AGENT | Cash-in client |
| POST | `/payments/agent-bank-deposits` | ADMIN | Dépôt banque agent |
| POST | `/payments/reversals` | ADMIN | Renversement transaction |

---

### 3.5 Payouts / Refunds

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| POST | `/payouts/requests` | ADMIN | Demander payout agent |
| POST | `/payouts/{payoutId}/complete` | ADMIN | Compléter payout |
| POST | `/payouts/{payoutId}/fail` | ADMIN | Échouer payout |
| POST | `/client-refunds/requests` | ADMIN | Demander refund client |
| POST | `/client-refunds/{refundId}/complete` | ADMIN | Compléter refund |
| POST | `/client-refunds/{refundId}/fail` | ADMIN | Échouer refund |

---

## 4) Lecture commune

### 4.1 Ledger

| Méthode | Endpoint | Rôle |
|---|---|---|
| GET | `/ledger/balance` | ADMIN, AGENT, MERCHANT, CLIENT |
| POST | `/ledger/transactions/search` | ADMIN, AGENT, MERCHANT, CLIENT |

Paramètres `POST /ledger/transactions/search` (body):
- `accountType`
- `ownerRef`
- `type`
- `status`
- `from`
- `to`
- `min`
- `max`
- `limit`
- `cursor`
- `sort`

---

## 5) Self-service (`/me`)

### 5.1 Client (`CLIENT`)

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/client/me/home` | Dashboard client |
| GET | `/client/me/profile` | Profil client |
| GET | `/client/me/balance` | Solde client |
| GET | `/client/me/cards` | Cartes du client |
| GET | `/client/me/transactions` | Historique transactions client |
| GET | `/client/me/transactions/{transactionRef}` | Détail transaction client |

Query params `/client/me/transactions`:
- `type`
- `status`
- `from`
- `to`
- `min`
- `max`
- `limit`
- `cursor`
- `sort`

---

### 5.2 Marchand (`MERCHANT`)

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/merchant/me/profile` | Profil marchand |
| GET | `/merchant/me/balance` | Solde marchand |
| GET | `/merchant/me/transactions` | Historique transactions marchand |
| GET | `/merchant/me/transactions/{transactionRef}` | Détail transaction marchand |
| GET | `/merchant/me/terminals` | Liste terminaux marchand |
| GET | `/merchant/me/terminals/{terminalUid}` | Détail terminal |

Query params `/merchant/me/transactions`:
- `type`
- `status`
- `from`
- `to`
- `min`
- `max`
- `limit`
- `cursor`
- `sort`

Query params `/merchant/me/terminals`:
- `status`
- `terminalUid`
- `limit`
- `cursor`
- `sort`

---

### 5.3 Agent (`AGENT`)

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/agent/me/summary` | Résumé agent |
| GET | `/agent/me/transactions` | Historique transactions agent |
| GET | `/agent/me/activities` | Activités agent |
| GET | `/agent/search` | Recherche agent |

Query params `/agent/me/transactions`:
- `type`
- `status`
- `from`
- `to`
- `min`
- `max`
- `limit`
- `cursor`
- `sort`

Query params `/agent/me/activities`:
- `from`
- `to`
- `limit`
- `cursor`
- `sort`

---

### 5.4 Terminal (`TERMINAL`)

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/terminal/me/status` | Statut terminal |
| GET | `/terminal/me/config` | Configuration terminal |
| GET | `/terminal/me/health` | Santé terminal |

---

## 6) Backoffice read (`ADMIN`)

### 6.1 Transactions

| Méthode | Endpoint |
|---|---|
| GET | `/backoffice/transactions` |
| GET | `/backoffice/transactions/{transactionRef}` |

Query params `/backoffice/transactions`:
- `query`
- `type`
- `status`
- `actorType`
- `actorRef`
- `terminalUid`
- `cardUid`
- `merchantCode`
- `agentCode`
- `clientPhone`
- `from`
- `to`
- `min`
- `max`
- `limit`
- `cursor`
- `sort`

---

### 6.2 Audit

| Méthode | Endpoint |
|---|---|
| GET | `/backoffice/audit-events` |

Query params:
- `action`
- `actorType`
- `actorRef`
- `resourceType`
- `resourceRef`
- `from`
- `to`
- `limit`
- `cursor`
- `sort`

---

### 6.3 Acteurs

| Méthode | Endpoint |
|---|---|
| GET | `/backoffice/agents` |
| GET | `/backoffice/clients` |
| GET | `/backoffice/merchants` |
| GET | `/backoffice/actors/{actorType}/{actorRef}` |


Query params listes:
- `query`
- `status`
- `createdFrom`
- `createdTo`
- `limit`
- `cursor`
- `sort`

---

### 6.4 Lookup

| Méthode | Endpoint |
|---|---|
| GET | `/backoffice/lookups` |

Query params:
- `q`
- `type`
- `limit`

---

## 7) Tri, filtres, pagination

### 7.1 `sort`
- `sort=createdAt`
- `sort=-createdAt`
- Défaut: `createdAt` décroissant

### 7.2 `cursor`
- Pagination cursor-based opaque

### 7.3 `limit`
- Valeur par défaut côté service
- Bornée au max autorisé

### 7.4 Filtres date/montant
- `from` / `to`: ISO-8601
- `min` / `max`: décimal

---

## 8) Erreurs standard

- `400`
- `401`
- `403`
- `404`
- `409`
- `422`

---

## 9) Compatibilité

Ce contrat est désormais **la référence unique**.
