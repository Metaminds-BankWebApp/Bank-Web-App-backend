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

## API Integration Prompt for Frontend

Use this prompt when frontend work needs to be aligned with backend APIs:

```text
Integrate frontend APIs with role-specific flows.

Use /api/auth/me as the single identity source after login.
Do not decode JWT manually for ownership.
Use role only for authorization and UI branching.

Public Customer flow:
- Use publicCustomerId from /api/auth/me.
- Make relevant [functionality] with permit with that ID.

Bank Officer flow:
- Use officerId from /api/auth/me.
- After creating a customer, resolve the created bankCustomerId.
- Save onboarding financial steps 2 to 5 using bankCustomerId.

Bank Customer flow:
- Use bankCustomerId from /api/auth/me.
- Make relevant [functionality] with permit by bankCustomerId.

Admin flow:
- Use roleName and roleId only for permission checks and UI access.

Keep API calls typed, preserve current forms, and handle 401 by refreshing token once.
```

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