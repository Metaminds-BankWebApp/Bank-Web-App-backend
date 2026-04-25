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
- Ensure Brevo SMTP login and key are valid:
  - `SPRING_MAIL_USERNAME`
  - `SPRING_MAIL_PASSWORD`
