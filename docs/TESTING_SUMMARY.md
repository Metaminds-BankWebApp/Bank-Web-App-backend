# Backend Testing Summary

## Purpose

The backend was tested module by module using JUnit 5, Mockito and AssertJ. The tests focus on the main business rules, validation, ownership and status changes instead of testing simple DTO getters or framework-generated code.

## Final Result

- Total test cases executed: **59**
- Passed: **59**
- Failed: **0**
- Errors: **0**
- External services used during tests: **None**

Run the complete suite from the backend project directory:

```powershell
.\mvnw.cmd test
```

The detailed Maven results are generated in `target/surefire-reports`.

## Module-by-Module Tests

### 1. CreditLens — 20 test cases

Test classes:

- `CreditEvaluationScoringServiceTest`
- `CreditEvaluationResponseServiceTest`
- `CreditEvaluationRecordServiceTest`

Main checks:

- Salary and business-income stability scoring
- Correct distribution of income-stability points
- Same salary rules for bank and public customers
- Monthly report selection
- Risk factors and positive-behaviour insight generation
- Efficient loading of financial report details

### 2. SpendIQ — 4 test cases

Test class: `ExpenseServiceTest`

Main checks:

- Creates and normalizes an expense category for the logged-in user
- Rejects duplicate categories for the same user
- Creates an expense only with a category owned by that user
- Prevents the same successful bank transaction from being imported twice

### 3. LoanSense — 4 test cases

Test class: `LoanEligibilityServiceTest`

Main checks:

- Generates an eligible personal-loan result from income and policy values
- Produces a not-eligible result when existing EMI exceeds available capacity
- Rejects an evaluation when monthly income is zero
- Rejects duplicate loan types in an officer evaluation request

### 4. Transactions — 4 test cases

Test class: `TransactionServiceTest`

Main checks:

- Initiates a valid transfer and stores a hashed OTP
- Enforces the minimum remaining account balance
- Completes a transfer after the correct OTP and updates both balances
- Marks the transaction failed after the third incorrect OTP

### 5. Bank Officer — 4 test cases

Test class: `BankOfficerCustomerOnboardingServiceTest`

Main checks:

- Continues bank-customer registration through the user service
- Normalizes NIC input and returns existing customer details
- Prevents an officer from editing another officer's completed customer
- Allows the owning officer to update contact details without changing account ownership

### 6. Admin — 8 test cases

Test classes:

- `BranchServiceTest`
- `AuditLogServiceSearchTest`

Main checks:

- Generates the next branch code and records an audit action
- Rejects a branch phone number already used in the system
- Prevents deletion of a branch linked to officers or customers
- Searches audit records without case sensitivity
- Searches audit status and displayed date values

## Additional Supporting Tests

The remaining tests cover password-reset OTP handling, global API errors, notifications, support conversations and the application entry point.

## Test Safety

All main module tests use mocked repositories and services. Running the normal test suite does not start the application, connect to Neon PostgreSQL or send real emails.
