# Kori API configuration guide

This project is configured to run in two Spring profiles without code changes between environments:

- `local` for developer workstations, with safe localhost defaults.
- `prod` for VPS / Docker deployments, with configuration supplied entirely by environment variables.

`local` is the default profile. Production must explicitly set `SPRING_PROFILES_ACTIVE=prod`.

## Audit summary

The original configuration contained environment-specific values directly in `src/main/resources/application.properties`:

- `server.port=8081`
- PostgreSQL URL, username and password pointing to `localhost`
- JWT issuer URI pointing to `http://localhost:8080/realms/kori`
- JWT audience hardcoded in `SecurityConfig`

Those values have been refactored so the codebase no longer needs manual edits when moving from local development to production.

## Configuration strategy

### Shared configuration

`src/main/resources/application.properties` now contains only shared settings and the default profile.

### Local profile

`src/main/resources/application-local.properties` provides local-friendly defaults:

- API port defaults to `8081`
- PostgreSQL defaults to `jdbc:postgresql://localhost:5432/kori`
- DB credentials default to `kori` / `kori`
- Keycloak issuer defaults to `http://localhost:8080/realms/kori`

Each of those values can still be overridden by environment variables.

### Production profile

`src/main/resources/application-prod.properties` requires environment variables for database access and JWT issuer configuration. No `localhost` value is embedded in the production profile.

## Required environment variables

### Common

| Variable | Description | Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring profile to run | `local` |
| `KORI_SERVER_PORT` | HTTP port used by the API | `8081` in `local`, `8080` in `prod` |
| `KORI_JWT_AUDIENCE` | Expected OAuth2 audience / client id | `kori-api` |

### Local

| Variable | Description | Default |
| --- | --- | --- |
| `KORI_DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/kori` |
| `KORI_DB_USERNAME` | PostgreSQL username | `kori` |
| `KORI_DB_PASSWORD` | PostgreSQL password | `kori` |
| `KORI_JWT_ISSUER_URI` | Keycloak realm issuer URI | `http://localhost:8080/realms/kori` |

### Local Docker Compose

| Variable | Description | Default     |
| --- | --- |-------------|
| `POSTGRES_DB` | Application database name | `kori`      |
| `POSTGRES_USER` | Application database user | `kori`      |
| `POSTGRES_PASSWORD` | Application database password | `change-me` |
| `KEYCLOAK_DB_NAME` | Dedicated Keycloak database name | `keycloak`  |
| `KEYCLOAK_DB_USERNAME` | Dedicated Keycloak database user | `keycloak`  |
| `KEYCLOAK_DB_PASSWORD` | Dedicated Keycloak database password | `keycloak`  |
| `KEYCLOAK_REALM` | Realm name expected by the API | `kori`      |
| `KC_BOOTSTRAP_ADMIN_USERNAME` | Local Keycloak bootstrap admin username | `admin`     |
| `KC_BOOTSTRAP_ADMIN_PASSWORD` | Local Keycloak bootstrap admin password | `admin`     |
| `KEYCLOAK_BIND_PORT` | Port exposed for local Keycloak access | `8080`      |

### Production

| Variable | Description |
| --- | --- |
| `KORI_DB_URL` | PostgreSQL JDBC URL reachable from the production runtime |
| `KORI_DB_USERNAME` | PostgreSQL username |
| `KORI_DB_PASSWORD` | PostgreSQL password |
| `KORI_JWT_ISSUER_URI` | Public or network-reachable Keycloak realm issuer URI |
| `KORI_SERVER_PORT` | Optional API port override |
| `KORI_JWT_AUDIENCE` | Optional JWT audience override |

## Run locally without Docker

1. Start PostgreSQL on `localhost:5432` with a `kori` database.
2. Start Keycloak on `http://localhost:8080` and create/import the `kori` realm.
3. Export variables only if you need to override the defaults.
4. Run the API:

```bash
./mvnw spring-boot:run
```

If you want to force the local profile explicitly:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

## Run locally with Docker Compose

1. Copy `.env.example` to `.env`.
2. Adjust the placeholder passwords and realm name if needed.
3. If you already have a Keycloak realm export, place it in `docker/keycloak/import/`.
4. Start the stack:

```bash
docker compose -f docker-compose.local.yml up --build
```

What the local stack does:

- starts one PostgreSQL container and creates both the application database and a dedicated Keycloak database
- starts Keycloak against PostgreSQL instead of the dev-file store, which makes local behavior closer to production
- starts Keycloak with `--import-realm`, so any realm export present in `docker/keycloak/import/` is imported automatically
- waits for PostgreSQL to become healthy before starting Keycloak
- waits for PostgreSQL and Keycloak healthchecks before starting the API

Realm import details:

- put a realm export file such as `kori-realm.json` or `realm-export.json` in `docker/keycloak/import/`
- the directory is mounted read-only into `/opt/keycloak/data/import`
- if no export file is present, Keycloak still starts normally
- if the realm already exists in the local PostgreSQL volume, Keycloak does not overwrite it automatically
- to force a clean re-import, remove the local Docker volume and start again

## Deploy in production

1. Provide environment variables on the VPS (shell, systemd, Compose env file, secret manager, etc.).
2. Set `SPRING_PROFILES_ACTIVE=prod`.
3. Build and run the container, or use the provided Compose example:

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

Production notes:

- the production Compose example expects PostgreSQL and Keycloak endpoints to be supplied externally
- no production code path depends on `localhost`
- you can point the same artifact to different environments by changing environment variables only

## Docker artifacts

- `Dockerfile`: multi-stage Maven build producing a small JRE runtime image.
- `docker-compose.local.yml`: local development stack with API + PostgreSQL + PostgreSQL-backed Keycloak + healthchecks + optional realm import.
- `docker-compose.prod.yml`: production-oriented API deployment driven entirely by environment variables.
- `docker/postgres/init/01-create-keycloak-db.sh`: initialization script that creates the dedicated local Keycloak database and user.
- `docker/keycloak/import/README.md`: documents where to place a local realm export for automatic import.

## Sensitive points / operational notes

- `KORI_JWT_ISSUER_URI` must be reachable from the API runtime. In Docker, `localhost` points to the container itself, not your host machine.
- `SecurityConfig` still validates the JWT audience, but the audience is now configurable through `KORI_JWT_AUDIENCE`.
- automatic realm import is intended for local bootstrap; once Keycloak data already exists in PostgreSQL, imports are not forced on every startup
- do not commit real secrets in `.env`; use `.env.example` as a template only
