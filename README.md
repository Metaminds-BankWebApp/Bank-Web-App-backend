# Primecore Backend

Primecore is a banking and personal finance application. This repository contains its Java REST API for customer onboarding, financial records, credit assessments, loan eligibility, expense tracking, transfers, and administrative workflows.

The backend supports four roles: **Admin**, **Bank Officer**, **Bank Customer**, and **Public Customer**. The frontend is maintained separately.

## Contents

- [Features](#features)
- [Technology stack](#technology-stack)
- [Local setup](#local-setup)
- [Development accounts](#development-accounts)
- [API usage](#api-usage)
- [Configuration](#configuration)
- [Project structure](#project-structure)
- [Build and tests](#build-and-tests)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

## Features

| Module | Implemented capabilities |
| --- | --- |
| Authentication | Email or username login, JWT access tokens, refresh-token rotation, logout, password-reset OTPs, and officer activation links. |
| Customer onboarding | Public-customer registration, officer-managed bank-customer onboarding, draft steps, financial records, and Sri Lankan NIC validation. |
| CreditLens | Bank and public customer credit evaluations, score factors, trends, insights, evaluation history, and PDF reports. |
| LoanSense | Officer-initiated bank-customer loan eligibility and affordability evaluations, product details, history, and configurable loan policies. |
| SpendIQ | Income and expense records, custom categories, monthly budgets, summaries, transaction-linked expenses, and PDF reports. |
| Transact | Beneficiaries, OTP-confirmed account transfers, cancellation, balances, transaction history, receipts, and PDF statements. |
| Bank officer workspace | Customer portfolios, financial-data entry, CRIB retrieval, transaction review, and work-queue cases. |
| Administration | Dashboard metrics, users, officers, branches, loan policies, risk adjustments, audit logs, and support management. |
| Notifications and support | In-app notifications, financial reminders, support conversations, read states, and scheduled conversation retention. |
| User profiles | Profile details and locally stored profile images. |

CRIB retrieval uses a separate PostgreSQL dataset through JDBC. The repository includes a sample dataset; it does not configure a live external credit-bureau API.

## Technology stack

Versions below reflect the dependencies declared in this repository.

| Area | Technology |
| --- | --- |
| Language and framework | Java 21, Spring Boot 4.0.5 |
| API and validation | Spring Web MVC, Jakarta Bean Validation |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL |
| Database migrations | Flyway; see the migration limitation below |
| Authentication | Spring Security, JJWT 0.12.6, BCrypt |
| API documentation | springdoc OpenAPI 2.8.6 and Swagger UI |
| Email | Brevo transactional email REST API |
| PDF generation | OpenPDF and Apache PDFBox |
| Build and tests | Maven Wrapper, JUnit Jupiter, Mockito, AssertJ |
| Container runtime | Multi-stage Docker build using Eclipse Temurin 21 |

## Local setup

### 1. Install prerequisites

- JDK 21, with `JAVA_HOME` pointing to the JDK.
- PostgreSQL and its command-line client (`psql`), or an equivalent database administration tool.
- Git. Maven is supplied through the wrapper; a separate Maven installation is optional.
- A Brevo account and verified sender when testing email-dependent flows.

Run the following from the repository directory:

```powershell
java -version
.\mvnw.cmd -version
```

On macOS or Linux, use `./mvnw` in place of `.\mvnw.cmd`. The first wrapper invocation needs internet access to download Maven and dependencies.

### 2. Prepare the application database

**Current limitation:** `src/main/resources/db/migration` contains duplicate Flyway versions, including `V5`, `V9`, and `V15`. The default configuration enables Flyway, so a normal startup can fail during migration validation. A clean, fully migrated database setup is not currently available until that history is reconciled.

For an existing development database, use a known compatible schema and disable Flyway while this issue remains. For a new, disposable local database, the steps below use Hibernate to create the entity tables as a limited bootstrap.

Create an empty database, for example:

```powershell
psql -h localhost -U postgres -c "CREATE DATABASE webapp;"
```

Hibernate bootstrap does **not** reproduce SQL-only objects, seed data, or every migration constraint. It is sufficient to begin local setup, but does not establish that every feature is ready. For example, the SQL migrations define a `branch_code_seq` sequence separately from the entity tables.

### 3. Configure the environment

Create a `.env` file in the repository root. The application imports this file as Java properties through `application.yaml`. Use plain `KEY=value` entries without shell-style `export` statements or surrounding quotes. Keep the file out of version control.

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/webapp
DB_USERNAME=postgres
DB_PASSWORD=replace-with-your-local-database-password
JWT_SECRET=replace-with-a-unique-random-secret-of-at-least-32-bytes

# Temporary local setup while duplicate migration versions remain.
SPRING_FLYWAY_ENABLED=false
SPRING_JPA_HIBERNATE_DDL_AUTO=update

PORT=8080
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
APP_TRANSACT_OTP_FAIL_OPEN_ENABLED=false
APP_TRANSACT_OTP_LOG_PLAIN_ENABLED=false
```

Replace the password and JWT placeholders. The JWT signing key must contain at least 32 UTF-8 bytes. Environment variables supplied by the shell or deployment platform take precedence over values imported from `.env`.

### 4. Start the backend and seed local accounts

```powershell
.\mvnw.cmd spring-boot:run
```

After Hibernate has created the tables, run this in a second terminal:

```powershell
psql -h localhost -U postgres -d webapp -v ON_ERROR_STOP=1 -f src/main/resources/db/seed/mock-logins.sql
```

This manual script creates the four roles and linked mock users, a branch, an officer, and a bank-customer account. It stores BCrypt password hashes and is outside the Flyway migration directory.

After the initial schema creation, stop the backend, change `SPRING_JPA_HIBERNATE_DDL_AUTO` to `validate`, and restart. Keep `SPRING_FLYWAY_ENABLED=false` until the migration history is repaired. Validation checks entity/schema compatibility; it does not replace the missing SQL migrations.

The default base URL is [http://localhost:8080](http://localhost:8080). This is an API server; the root path is not a frontend page or a health endpoint.

### 5. Optional: configure the CRIB dataset

Create a separate database named `Crib_DB` and import `src/main/resources/db/crib/Crib_Dataset.sql` into it using `psql` or your database administration tool.

**The dataset script drops and recreates its CRIB tables. Use a dedicated sample database, and do not rerun it over data you need to keep.**

Add these entries to `.env`, then restart:

```dotenv
CRIB_DB_URL=jdbc:postgresql://localhost:5432/Crib_DB
CRIB_DB_USERNAME=postgres
CRIB_DB_PASSWORD=replace-with-your-local-database-password
CRIB_DB_SCHEMA=public
```

The main application database and CRIB database are separate connections. CRIB linking and retrieval require the secondary connection and its dataset.

## Development accounts

After running `mock-logins.sql`, the following accounts are available. All use the password **`Demo@1234`**.

| Role | Email | Username |
| --- | --- | --- |
| `ADMIN` | `admin.mock@example.test` | `admin.mock` |
| `BANK_OFFICER` | `officer.mock@example.test` | `officer.mock` |
| `BANK_CUSTOMER` | `bank.customer.mock@example.test` | `bank.customer.mock` |
| `PUBLIC_CUSTOMER` | `public.customer.mock@example.test` | `public.customer.mock` |

These accounts are for development or staging. Remove or change known-password accounts before allowing public access. Their email addresses are placeholders and cannot receive real email.

The older `V3__seed_roles_and_demo_users.sql` contains the `*.demo@primecore.local` users referenced in earlier documentation. Do not assume those logins work after a fresh bootstrap: Flyway is disabled in the setup above, and the startup hook in `PasswordMigrationService` that converted plaintext passwords to BCrypt is currently commented out.

Additional optional seed scripts are available in `src/main/resources/db/seed`:

- `mock-bank-customer-transactions.sql`: beneficiaries and completed transactions.
- `mock-bank-customer-financial-records.sql`: financial records, income, loans, cards, liabilities, and missed payments.

Both additional scripts expect the five customers `bank.customer.01` through `bank.customer.05` and their related records to exist already. The four-role login script does not create those five customers.

## API usage

Once the application starts, API documentation is configured at:

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)

### Authenticate and resolve the current user

Send `POST /api/auth/login` with an email or username in `identifier`:

```json
{
  "identifier": "bank.customer.mock@example.test",
  "password": "Demo@1234"
}
```

The response includes `accessToken`, `tokenType`, `refreshToken`, `expiresIn`, and user details. Send the access token on protected requests:

```http
GET /api/auth/me HTTP/1.1
Host: localhost:8080
Authorization: Bearer <accessToken>
```

`GET /api/auth/me` is the canonical identity endpoint. It returns `userId`, role details, and applicable ownership IDs: `bankCustomerId`, `publicCustomerId`, and `officerId`. These domain IDs are distinct from the user ID.

Use `POST /api/auth/refresh` with `{"refreshToken":"<refreshToken>"}` to rotate tokens. `POST /api/auth/logout` accepts the same request shape and revokes the refresh token.

### Main route groups

| Prefix | Purpose |
| --- | --- |
| `/api/auth` | Registration, login, tokens, password reset, activation, and current identity |
| `/api/users/profile` | User profile and profile image operations |
| `/api/public-customers` | Public-customer onboarding and financial records |
| `/api/bank-officers/customers` | Customer portfolios, onboarding, and financial-data workflows |
| `/api/bank-officers/work-queue/cases` | Officer case tracking |
| `/api/creditlens` | Credit evaluations, reports, trends, and insights |
| `/api/loansense` | Bank-customer loan eligibility and officer evaluations |
| `/api/spendiq` | Expenses, incomes, categories, budgets, and reports |
| `/api/bank-customers/transact` | Customer transfers, OTP verification, beneficiaries, and statements |
| `/api/bank-officers/transact` | Officer transaction review |
| `/api/admin` | Administration, policy settings, audit logs, and support |
| `/api/notifications` | User notifications and read states |
| `/api/support` | Support requests, conversations, and messages |

Access depends on the HTTP method, role, and record ownership. Public registration, login, password-reset, and selected activation endpoints do not require an access token. Consult the controllers, `SecurityConfig`, and service checks when integrating a route; the OpenAPI role labels do not describe every public onboarding exception or ownership rule.

## Configuration

Configuration is defined in `src/main/resources/application.yaml`, `application.properties`, and the production override `application-prod.yaml`.

| Variable | Purpose |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Primary PostgreSQL JDBC connection |
| `JWT_SECRET` | JWT signing key; set a unique secret for each environment |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Access-token lifetime; default `3600000` (1 hour) |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | Refresh-token lifetime; default `1209600000` (14 days) |
| `PORT` | HTTP port; default `8080` |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated exact frontend origins; defaults to localhost and 127.0.0.1 on port 3000 |
| `CRIB_DB_URL`, `CRIB_DB_USERNAME`, `CRIB_DB_PASSWORD` | Secondary CRIB database connection |
| `CRIB_DB_SCHEMA` | CRIB schema; default `public` |
| `BREVO_API_KEY`, `APP_MAIL_FROM` | Brevo API key and verified sender address |
| `APP_MAIL_NAME` | Sender display name; default `Primecore` |
| `APP_FRONTEND_ACTIVATION_URL` | Frontend activation/reset page; default `http://localhost:3000/reset-password` |
| `APP_TRANSACT_OTP_OVERRIDE_RECIPIENT_EMAIL` | Local transfer-OTP testing recipient; leave unset in production |
| `APP_TRANSACT_OTP_FAIL_OPEN_ENABLED` | Allows a transfer initiation to continue after email delivery failure; keep `false` for normal operation |
| `APP_TRANSACT_OTP_LOG_PLAIN_ENABLED` | Allows plaintext OTP logging in the email-failure fallback; keep `false` for normal operation |
| `APP_STORAGE_PROFILE_IMAGES_DIR` | Local profile-image directory; default `uploads/profile-images` |

For local schema controls, use `SPRING_FLYWAY_ENABLED` and `SPRING_JPA_HIBERNATE_DDL_AUTO`. The `prod` profile also exposes `FLYWAY_ENABLED` and `HIBERNATE_DDL_AUTO`; see the deployment guide.

### Email and OTP testing

Add the following to `.env` when testing email delivery:

```dotenv
BREVO_API_KEY=replace-with-your-brevo-api-key
APP_MAIL_FROM=your-verified-sender@example.com
APP_MAIL_NAME=Primecore
APP_TRANSACT_OTP_OVERRIDE_RECIPIENT_EMAIL=your-real-test-inbox@example.com
```

The email service sends plain-text messages directly through Brevo; no Brevo template is configured. The recipient override applies to **transfer OTPs only**, so password-reset and activation testing need an account with a deliverable email address.

Transfer OTPs are hashed in `transaction_otp_logs`, expire after five minutes, and allow up to three incorrect verification attempts. These are separate from password-reset OTPs, whose default expiry is ten minutes.

### Profile images and scheduled work

Profile images are written to the local filesystem and served under `/profile-images/**`. Multipart limits are 5 MB per file and 6 MB per request. Container deployments need a writable, persistent directory if uploads must survive replacement of an instance.

Scheduling is enabled for financial reminders and support retention. Support retention defaults to 30 days, with cleanup scheduled at 03:15 in `Asia/Colombo`; its settings are in `application.properties`.

## Project structure

```text
src/main/java/com/bank_web_app/backend/
  admin/           Administration, branches, policies, and audit logs
  auth/            Login, tokens, password reset, and activation
  bankcustomer/    Bank-customer accounts and financial records
  bankofficer/     Onboarding, portfolios, and work queues
  common/          Configuration, email, errors, and shared utilities
  creditlens/      Credit scoring, insights, and reports
  crib/            Separate CRIB datasource and dataset lookup
  loansense/       Loan eligibility and affordability
  notification/    Notifications, events, and reminders
  publiccustomer/  Public-customer onboarding and financial records
  security/        JWT authentication
  spendiq/         Income, expenses, categories, and budgets
  support/         Support conversations and retention
  transact/        Transfers, OTPs, beneficiaries, and receipts
  user/            Users, roles, profiles, and image storage
src/main/resources/
  db/migration/    Application Flyway SQL history
  db/seed/         Manual development data
  db/crib/         Separate CRIB sample dataset and SQL updates
src/test/java/     Backend tests
docs/             Testing notes and deployment guide
Dockerfile        Java 21 build and runtime image
pom.xml           Maven dependencies and build configuration
```

Modules generally separate controllers, request/response DTOs, services, entities, repositories, and mappers. Controllers expose HTTP operations, services enforce business rules and ownership, and repositories handle persistence.

## Build and tests

Run commands from the repository root:

| Task | PowerShell command |
| --- | --- |
| Compile | `.\mvnw.cmd -DskipTests compile` |
| Run tests | `.\mvnw.cmd test` |
| Build executable JAR, including tests | `.\mvnw.cmd clean package` |
| Run the packaged application | `java -jar target/backend-0.0.1-SNAPSHOT.jar` |

The current tests use mocks and cover business rules including credit scoring, loan eligibility, transaction OTPs, ownership, onboarding, password reset, administration, notifications, and support. The application entry-point test does not start a Spring context. These tests do not establish that PostgreSQL migrations, email delivery, or deployed integrations work.

Maven writes test results to `target/surefire-reports`. The [testing summary](docs/TESTING_SUMMARY.md) records an earlier run; use fresh Maven results for the current test count and status.

## Deployment

See the [deployment guide](docs/DEPLOYMENT.md) for Docker, the production profile, Neon database connections, migration limitations, and database transfer commands.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Flyway reports more than one migration with a version | Duplicate migration versions remain in the repository. Follow the limited local bootstrap above; reconcile migration history before using Flyway for a release. |
| Missing JWT configuration or an invalid signing-key error | Set `JWT_SECRET` to a random secret containing at least 32 UTF-8 bytes. |
| Database connection or authentication failure | Check the JDBC URL, credentials, database existence, and network access. |
| Hibernate reports missing tables or columns | The database does not match the entities. Verify which schema was restored or bootstrapped; `validate` does not create tables. |
| Mock login fails | Confirm `mock-logins.sql` ran against the application's database and the account is active. Legacy plaintext demo passwords are not automatically migrated. |
| Email or transfer OTP delivery fails | Check the Brevo key, verified sender, and a routable recipient. Mock addresses cannot receive email. |
| CRIB lookup reports missing configuration | Set the secondary datasource variables and import the sample dataset into its own database. |
| Browser requests fail because of CORS | Set the exact frontend origin, including scheme and port, in `APP_CORS_ALLOWED_ORIGINS`. |
| Swagger fails while the API starts | Check startup/request logs and compatibility of the declared Spring Boot and springdoc versions; a unit-test pass does not verify Swagger at runtime. |
| Profile-image uploads fail or disappear after deployment | Configure a directory writable by the runtime user and mount persistent storage. |
