# Bank-Web-App-backend

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