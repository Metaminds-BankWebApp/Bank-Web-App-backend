# Bank-Web-App-backend

## Backend Logic Placeholder

This repository already contains the main backend scaffolding. Use this section as the checklist for future API logic work:

- Implement and keep `/api/auth/me` as the canonical identity endpoint.
- Keep role checks separate from ownership checks.
- Store ownership using domain IDs such as `bankCustomerId`, `publicCustomerId`, and `officerId`.
- Keep Swagger role notes accurate for public and protected routes.
- Add or extend DTOs in `src/main/java/com/bank_web_app/backend/**/dto` before wiring new endpoints.
- Add service methods first, then controller routes, then Swagger annotations, then frontend integration.
- Validate new flows with `mvnw.cmd -q -DskipTests compile` before wiring the frontend.

## Development Demo Logins

The following demo users are seeded by Flyway migration `V3__seed_roles_and_demo_users.sql`.

Important:
- Seeded password value is `Demo@1234`.
- Passwords are automatically migrated to BCrypt at runtime by `PasswordMigrationService`.
- These credentials are for local development only.

| Role | Email | Username | Password |
|---|---|---|---|
| ADMIN | admin.demo@primecore.local | admin.demo | Demo@1234 |
| BANK_OFFICER | officer.demo@primecore.local | officer.demo | Demo@1234 |
| BANK_CUSTOMER | bank.customer.demo@primecore.local | bank.customer.demo | Demo@1234 |
| PUBLIC_CUSTOMER | public.customer.demo@primecore.local | public.customer.demo | Demo@1234 |

## Transact OTP Email Notes (Brevo)

- OTP is generated and saved in `transaction_otp_logs` first, then email delivery is attempted.
- Demo user emails use `@primecore.local`, which is non-routable for real inbox delivery.
- For local OTP testing with a real inbox, set:
  - `APP_TRANSACT_OTP_OVERRIDE_RECIPIENT_EMAIL=<your-real-email>`
- Ensure Brevo sender is verified:
  - `APP_MAIL_FROM=<brevo-verified-sender-email>`
- Ensure Brevo API key is valid:
  - `BREVO_API_KEY`

## Deploying with Neon PostgreSQL

The primary application database uses `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
Set those values, along with `SPRING_PROFILES_ACTIVE=prod`, as secrets in the
deployment provider. Copy the database host, database name, role, and password
from **Neon Dashboard -> Connect**. The JDBC URL must include Neon SSL options:

```text
jdbc:postgresql://<neon-host>/<database>?sslmode=require&channel_binding=require
```

Use the **pooled** Neon endpoint for `DB_URL` in normal application instances.
Set `FLYWAY_ENABLED=true` and `FLYWAY_DB_URL` to the **direct (non-pooler)**
endpoint only in one release/migration job. This avoids every scaled instance
contending to run schema migrations. Production Hibernate only validates the
schema; it never creates or changes production tables.

### One-time JPA bootstrap for an empty Neon database

When intentionally starting with an empty Neon database, set
`HIBERNATE_DDL_AUTO=update` for one startup. Hibernate creates tables represented
by JPA entities while Flyway remains disabled. On a successful startup, change
the variable back to `HIBERNATE_DDL_AUTO=validate` and restart.

This approach does not create database objects defined only in SQL migrations
and does not seed demo users. It is a bootstrap fallback while the duplicate
Flyway migration versions are being resolved.

### Mock logins for development or staging

After the schema exists, run `src/main/resources/db/seed/mock-logins.sql` in
the Neon SQL Editor to create one mock login for each application role. It is
manual-only and idempotent; it is deliberately outside Flyway so those accounts
cannot be added accidentally to a production deployment.

| Role | Login email | Password |
|---|---|---|
| ADMIN | `admin.mock@example.test` | `Demo@1234` |
| BANK_OFFICER | `officer.mock@example.test` | `Demo@1234` |
| BANK_CUSTOMER | `bank.customer.mock@example.test` | `Demo@1234` |
| PUBLIC_CUSTOMER | `public.customer.mock@example.test` | `Demo@1234` |

Remove or change these known-password accounts before public access.

To move the existing local `webapp` database, install PostgreSQL client tools
(`pg_dump` and `pg_restore`) and run the following in PowerShell. Paste the
direct Neon PostgreSQL connection string only when prompted; do not store it in
the repository.

```powershell
pg_dump -Fc --no-owner --no-privileges -h localhost -U postgres -d webapp -f webapp.dump
pg_restore -v --no-owner --no-privileges -d "postgresql://<role>:<password>@<direct-neon-host>/<database>?sslmode=require&channel_binding=require" webapp.dump
```

Verify the import with:

```powershell
psql "postgresql://<role>:<password>@<direct-neon-host>/<database>?sslmode=require&channel_binding=require" -c "SELECT count(*) FROM users;"
```

`crib.datasource` is a separate database connection. Import
`src/main/resources/db/crib/Crib_Dataset.sql` into the Neon `Crib_DB` database,
then set `CRIB_DB_URL` to its pooled JDBC URL. By default it reuses
`DB_USERNAME` and `DB_PASSWORD`; set `CRIB_DB_USERNAME` and
`CRIB_DB_PASSWORD` only if the CRIB database uses a different Neon role.
