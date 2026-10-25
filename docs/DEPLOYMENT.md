# Deployment guide

This guide describes the configuration supplied by the Primecore backend repository. Complete [local setup](../README.md#local-setup) first and verify the target database schema before deploying.

## Current migration limitation

The Flyway directory contains duplicate versions. Do not enable Flyway on a release until the migration history has been reconciled and checked against the target database's existing history. Renaming already-applied migrations without reviewing that history can introduce further inconsistencies.

The production profile defaults to Flyway disabled and Hibernate `validate`. It therefore expects an existing, compatible schema; an empty database will not be initialized automatically.

For a disposable development or staging database only, `HIBERNATE_DDL_AUTO=update` can bootstrap JPA entity tables for one startup with Flyway disabled. Set it back to `validate` afterward. This omits SQL-only objects, migration constraints, and seed data, so it is not a complete production schema provisioning process.

## Production configuration

Set these variables through the deployment provider's environment or secret store:

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<database-host>/<database>?sslmode=require
DB_USERNAME=<database-role>
DB_PASSWORD=<database-password>
JWT_SECRET=<unique-random-secret-of-at-least-32-bytes>
HIBERNATE_DDL_AUTO=validate
FLYWAY_ENABLED=false
APP_CORS_ALLOWED_ORIGINS=https://<frontend-host>
APP_FRONTEND_ACTIVATION_URL=https://<frontend-host>/reset-password
APP_TRANSACT_OTP_FAIL_OPEN_ENABLED=false
APP_TRANSACT_OTP_LOG_PLAIN_ENABLED=false
BREVO_API_KEY=<brevo-api-key>
APP_MAIL_FROM=<verified-sender-address>
APP_MAIL_NAME=Primecore
```

These are placeholders. Do not commit real credentials. Keep the local-only `APP_TRANSACT_OTP_OVERRIDE_RECIPIENT_EMAIL` unset. Remove local schema overrides such as `SPRING_JPA_HIBERNATE_DDL_AUTO=update` from the deployment environment.

The server reads `PORT`, defaulting to `8080`. Configure frontend origins exactly, without URL paths. Profile images require a writable persistent directory configured through `APP_STORAGE_PROFILE_IMAGES_DIR`; the container's default working directory is not guaranteed to be writable by the runtime user.

For CRIB-enabled workflows, also set `CRIB_DB_URL`. In the production profile, CRIB credentials default to `DB_USERNAME` and `DB_PASSWORD`; set `CRIB_DB_USERNAME` and `CRIB_DB_PASSWORD` if the secondary database uses another role. `CRIB_DB_SCHEMA` defaults to `public`.

## Docker

The Dockerfile builds the JAR with the Maven Wrapper in a Java 21 JDK image, then runs it in a Java 21 JRE image as the `spring` user. The image build skips tests, so run the test suite separately before building a release.

```powershell
.\mvnw.cmd test
docker build -t primecore-backend .
docker run --rm --name primecore-backend -p 8080:8080 --env-file .env.production primecore-backend
```

Create the ignored `.env.production` file with the production variables above and valid values before running the container. Docker reads that file through `--env-file`; do not assume the application imports `.env.production` automatically. A database URL using `localhost` refers to the container itself, so use a database hostname reachable from the container.

For uploads, mount persistent storage at a path writable by the `spring` user and set `APP_STORAGE_PROFILE_IMAGES_DIR` to that path. Multi-instance deployments also need a storage arrangement that makes uploaded files accessible to every instance serving them.

## Neon PostgreSQL

Copy the host, database name, role, and password from the target Neon project's connection details. Use a PostgreSQL **JDBC** URL for the application:

```text
jdbc:postgresql://<pooled-neon-host>/<database>?sslmode=require&channel_binding=require
```

Use the pooled endpoint for `DB_URL`. The production connection-pool settings can be adjusted with `DB_POOL_MAX_SIZE` (default `10`) and `DB_POOL_MIN_IDLE` (default `2`).

After the duplicate migration versions have been resolved, run migrations in one controlled release job using the direct, non-pooler endpoint:

```dotenv
FLYWAY_ENABLED=true
FLYWAY_DB_URL=jdbc:postgresql://<direct-neon-host>/<database>?sslmode=require&channel_binding=require
```

Flyway credentials default to the primary database credentials; `FLYWAY_DB_USERNAME` and `FLYWAY_DB_PASSWORD` can override them. Keep Flyway disabled in ordinary application instances and Hibernate set to `validate`. Flyway is configured as part of application startup; this repository does not provide a separate migration-only job definition.

### Copy an existing local database

Install PostgreSQL client tools and choose an empty target database. These commands export the local `webapp` database and restore it using a direct Neon connection. Replace the host, role, and database placeholders; the tools prompt for passwords.

```powershell
pg_dump -Fc --no-owner --no-privileges -h localhost -U postgres -W -d webapp -f webapp.dump
pg_restore -v --no-owner --no-privileges --exit-on-error -W -d "postgresql://<role>@<direct-neon-host>/<database>?sslmode=require&channel_binding=require" webapp.dump
psql -W "postgresql://<role>@<direct-neon-host>/<database>?sslmode=require&channel_binding=require" -c "SELECT count(*) FROM users;"
```

Treat the dump as sensitive data and keep it outside version control. The row-count query is only a basic import check; also verify schema compatibility and the application workflows you use before switching environments.

Import `src/main/resources/db/crib/Crib_Dataset.sql` into a separate, dedicated CRIB sample database if required. That script drops and recreates its CRIB tables. Point `CRIB_DB_URL` at the secondary database's pooled JDBC URL. The application-database dump does not include this separate database.

## Development and staging data

For an isolated development or staging deployment, run `src/main/resources/db/seed/mock-logins.sql` after the schema exists. It is manual and idempotent; it is not a Flyway migration. See the [development accounts](../README.md#development-accounts) for login details and the prerequisites for additional seed scripts.

Remove or change known-password accounts before public access. Existing Flyway SQL includes demo/sample-data migrations as well, so review those when repairing the release migration history.
