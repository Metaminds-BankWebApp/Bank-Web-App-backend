# PrimeCore Banking Backend

[Live application](https://bank-web-app-theta.vercel.app/) · [Frontend repository](https://github.com/Metaminds-BankWebApp/bank-web-app-frontend) · [Frontend setup](https://github.com/Metaminds-BankWebApp/bank-web-app-frontend#run-locally)

The PrimeCore backend powers the banking services behind the PrimeCore web app. It manages customer records, spending and budgets, credit assessments, loan eligibility, transfers, and the everyday work of bank officers and administrators.

This repository contains the Spring Boot REST API. The Next.js frontend is maintained separately in the linked repository.

## Explore the project

- [User roles](#who-it-supports)
- [SpendIQ](#spendiq-spending-and-budgets), [CreditLens](#creditlens-credit-health-and-assessments), [LoanSense](#loansense-borrowing-eligibility), and [Transact](#transact-transfers-and-payment-records)
- [Technology](#technology)
- [Run locally](#run-locally)
- [Connect the frontend](#connect-the-frontend)
- [Build and test](#build-and-test)
- [Deployment](#deployment)
- [Repository structure](#repository-structure)

## Who it supports

| User | Main capabilities |
| --- | --- |
| Public customers | Register, provide financial information, track spending, and review self credit assessments. |
| Bank customers | Manage spending, view bank credit assessments, explore loan eligibility, and transfer money. |
| Bank officers | Onboard customers, maintain financial records, review credit information, and manage assigned work. |
| Administrators | Manage users, branches, officers, lending policies, audit records, and support conversations. |

## SpendIQ: spending and budgets

SpendIQ helps customers keep a record of their spending and understand how it compares with their plans.

- Record, update, delete, and review expenses and income.
- Organize spending into categories.
- Review spending summaries and category breakdowns.
- Set budget limits and monitor spending against them.
- Carry budget limits into a new month through budget rollover.
- Review past activity and generate downloadable spending reports.

## CreditLens: credit health and assessments

CreditLens uses customer financial records to produce credit evaluations and supporting insights.

- Generate self credit assessments for public customers.
- Support bank customer credit evaluations and officer reviews.
- Provide credit scores, risk levels, and explanations of contributing factors.
- Supply dashboard summaries, trends, and financial insights.
- Keep previous evaluations so customers and officers can review changes over time.
- Generate monthly report information and downloadable credit report PDFs.

## LoanSense: borrowing eligibility

LoanSense evaluates borrowing options for bank customers using financial information, credit assessments, and the bank's lending policies.

- Assess eligibility for personal, vehicle, education, and housing loans.
- Calculate recommended maximum borrowing amounts.
- Provide estimated monthly repayments and applicable repayment periods.
- Consider income, existing commitments, and risk when assessing repayment capacity.
- Explain eligibility outcomes for each loan category.
- Keep assessment history for later review.

## Transact: transfers and payment records

Transact supports customer transfers and keeps the records needed to review completed activity.

- Verify account details during banking workflows.
- Request and verify one-time passwords for transfers.
- Process transfers and record transaction references.
- Add, update, list, and remove beneficiaries.
- Retrieve transaction details and payment history.
- Download individual transaction receipts and transaction statements as PDFs.
- Provide bank officers with transaction review capabilities.

## Customer onboarding and financial records

The backend supports both public customer onboarding and bank officer-assisted customer registration.

- Capture customer details and financial information.
- Record income, loans, credit cards, liabilities, and missed payments.
- Save onboarding steps as drafts and resume incomplete work.
- Track progress through financial information, credit information, and final review steps.
- Look up credit information from the configured CRIB dataset using a customer's national identity number.
- Record credit information request and retrieval status.
- Maintain historical financial snapshots when customer information changes.

## Bank officer operations

- Browse customer portfolios and open customer records.
- Manage onboarding and financial maintenance work.
- Follow customer cases through a work queue.
- Review financial history and credit evaluations.
- Access customer transaction records.

## Administration and oversight

Administrators manage the records and policies that support banking operations.

- Manage user accounts and bank officer records.
- Create and manage branches.
- Maintain lending policies for supported loan categories.
- Configure risk adjustments used in lending assessments.
- Review administrative dashboard information.
- Follow recorded activity through audit logs.
- Oversee support conversations and update their status.

## Account access and profiles

- Registration and sign-in for supported account roles.
- Account activation workflows and related email messages.
- Password recovery with one-time password verification.
- Access checks based on account roles and ownership of customer records.
- Profile updates and profile image storage.

## Notifications and support

- Deliver notifications about relevant account and banking activity.
- Send email messages for supported activation, account, and verification workflows.
- Create support conversations with a subject, category, and message.
- Allow users and support staff to exchange replies.
- Track read status and whether a conversation is open or closed.

## Reports and history

PrimeCore keeps spending, credit, financial, and transaction records available for later review. Downloadable reports include spending reports, credit assessment reports, transaction statements, and payment receipts.

Available features depend on the user's role, customer records, and configured banking and email services.

## Technology

| Area | Tools |
| --- | --- |
| Runtime and framework | Java 21, Spring Boot 4.0.5 |
| API and access control | Spring MVC, Spring Security, JWT |
| Data | PostgreSQL, Spring Data JPA, Flyway |
| API documentation | Springdoc OpenAPI / Swagger UI |
| PDF reports | OpenPDF, Apache PDFBox |
| Email | Brevo HTTP API |
| Build and tests | Maven Wrapper, JUnit, Mockito, AssertJ |
| Container | Docker with Eclipse Temurin 21 |

Versions and dependencies are defined in [pom.xml](pom.xml).

## Run locally

Install JDK 21 and make it available through `JAVA_HOME` or `PATH`. Use the included Maven Wrapper; a separate Maven installation is not required. You also need a PostgreSQL database with the current application schema and reference data.

### Database setup limitation

The [migration directory](src/main/resources/db/migration) currently contains duplicate Flyway versions, including `V5`, `V9`, and `V15`. Flyway cannot apply this directory as a valid migration sequence until the version conflicts are reconciled with the database's migration history. Do not treat this checkout as a working automatic setup for an empty database.

For now, use a prepared development database supplied by the team. The commands below disable Flyway and validate the existing schema; they do not create tables or seed accounts. The default configuration otherwise enables Flyway and Hibernate schema updates. Seed scripts in [db/seed](src/main/resources/db/seed) are optional development fixtures, not a complete database bootstrap.

### Start against a prepared database

From the backend repository root, set these values in PowerShell, replacing the placeholders with your own development credentials:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/primecore'
$env:DB_USERNAME = '<database-user>'
$env:DB_PASSWORD = '<database-password>'
$env:JWT_SECRET = '<random-secret-of-at-least-32-UTF-8-bytes>'
$env:JWT_ACCESS_TOKEN_EXPIRATION_MS = '3600000'
$env:SPRING_FLYWAY_ENABLED = 'false'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
$env:APP_CORS_ALLOWED_ORIGINS = 'http://localhost:3000,http://127.0.0.1:3000'
$env:APP_FRONTEND_ACTIVATION_URL = 'http://localhost:3000/reset-password'
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, export the same environment variables and run `./mvnw spring-boot:run`. The default port is `8080`; set `PORT` to change it. The API base URL is `http://localhost:8080/api`.

The application also imports an optional root `.env` file as Java properties through [application.yaml](src/main/resources/application.yaml). Use `KEY=value` entries without shell commands or surrounding quotes. Keep database passwords, JWT secrets, and email API keys out of Git.

### Email and credit dataset configuration

| Variable | Purpose |
| --- | --- |
| `BREVO_API_KEY` | Brevo API key for email delivery |
| `APP_MAIL_FROM` | Sender address configured for email delivery |
| `APP_MAIL_NAME` | Sender display name |
| `APP_FRONTEND_ACTIVATION_URL` | Frontend activation/password setup page; local value shown above |
| `CRIB_DB_URL` | JDBC URL for the separate CRIB dataset database |
| `CRIB_DB_USERNAME`, `CRIB_DB_PASSWORD` | Credentials for the CRIB dataset database |
| `CRIB_DB_SCHEMA` | Dataset schema; normally `public` |

Activation, password recovery, and transfer OTP delivery require working email configuration. CRIB lookup workflows require the separate dataset; the [CRIB SQL files](src/main/resources/db/crib) are maintained separately from the main application migrations.

## Connect the frontend

Clone the [frontend repository](https://github.com/Metaminds-BankWebApp/bank-web-app-frontend), or use your existing checkout. The Next.js application is inside its `frontend/` directory. Create `frontend/.env.local` there with:

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

Include `/api`: the frontend appends paths such as `/auth/login` to this value. From the frontend repository root, run:

```bash
cd frontend
npm ci
npm run dev
```

Open [localhost:3000](http://localhost:3000) and sign in with an account configured in your development database. The frontend attaches a bearer token to authenticated API requests and enables credentials. The backend must allow the exact frontend origin through `APP_CORS_ALLOWED_ORIGINS`.

The backend exposes `POST /api/auth/login` for sign-in and `GET /api/auth/me` for the authenticated identity and role-specific customer/officer IDs. API documentation is configured at [Swagger UI](http://localhost:8080/swagger-ui.html) and [OpenAPI JSON](http://localhost:8080/v3/api-docs) when the backend is running.

## Build and test

Run from the backend repository root:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Use `./mvnw` on macOS or Linux. `package` runs tests and creates the executable JAR in `target/`. Test results are written to `target/surefire-reports/`. See [the testing summary](docs/TESTING_SUMMARY.md) for documented coverage; its recorded totals describe a previous run.

## Deployment

The [Dockerfile](Dockerfile) builds the backend with Java 21 and runs the packaged JAR. The [production profile](src/main/resources/application-prod.yaml) contains database pooling and schema validation settings for deployment, including Render/Neon configuration.

- Set `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in the deployment environment. Database URLs must use PostgreSQL JDBC syntax and the database provider's required TLS settings.
- Set `APP_CORS_ALLOWED_ORIGINS=https://bank-web-app-theta.vercel.app`. For additional frontend origins, use a comma-separated list of exact origins without trailing slashes.
- Set `APP_FRONTEND_ACTIVATION_URL=https://bank-web-app-theta.vercel.app/reset-password` so activation emails open the deployed frontend.
- Configure the email and CRIB variables above for the workflows you use.
- Set the frontend's `NEXT_PUBLIC_API_BASE_URL` to the deployed backend's HTTPS URL ending in `/api`, then rebuild/redeploy the frontend.
- The production profile defaults to `HIBERNATE_DDL_AUTO=validate` and `FLYWAY_ENABLED=false`. Prepare the schema separately; resolve the migration conflicts described above before enabling a migration job. Separate `FLYWAY_DB_URL`, `FLYWAY_DB_USERNAME`, and `FLYWAY_DB_PASSWORD` values are supported for migrations.
- Profile images default to `uploads/profile-images`. Use persistent writable storage, configured through `APP_STORAGE_PROFILE_IMAGES_DIR`, if uploads must survive container replacement.

## Repository structure

```text
Bank-Web-App-backend/
|-- src/main/java/com/bank_web_app/backend/
|   |-- auth/, security/, user/       # Authentication, access, and profiles
|   |-- spendiq/, creditlens/         # Spending and credit assessments
|   |-- loansense/, transact/         # Loan eligibility and transfers
|   |-- bankofficer/, admin/          # Staff operations
|   |-- bankcustomer/, publiccustomer/
|   |-- crib/, notification/, support/, common/
|-- src/main/resources/
|   |-- application.yaml             # Base configuration
|   |-- application.properties       # Port, CORS, uploads, support retention
|   |-- application-prod.yaml        # Production overrides
|   |-- db/                          # Migrations, development seeds, CRIB SQL
|-- src/test/                        # Backend tests
|-- docs/TESTING_SUMMARY.md
|-- Dockerfile
|-- pom.xml
|-- mvnw / mvnw.cmd
```
