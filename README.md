# Utility Billing System (Backend Only)

**REST API backend only — no frontend application is included or required.**

Spring Boot backend for WASAC/REG utility billing. Test using **Swagger UI** at `http://localhost:8081/swagger-ui.html`.

> **Full Swagger guide:** see [SWAGGER_GUIDE.md](SWAGGER_GUIDE.md) — login, authorize, role switching, step-by-step workflow, and sample request bodies.

## Tech Stack

- Java 17, Spring Boot 4.0.6
- Spring Data JPA, Spring Security (JWT)
- PostgreSQL, JavaMail (Gmail SMTP)
- Swagger/OpenAPI (springdoc)

## Prerequisites

1. **Java 17+**
2. **PostgreSQL**
   ```sql
   CREATE DATABASE utility_billing_db;
   ```
3. **Gmail App Password** for sending emails (see Email Setup below)

## Run the Application

```bash
./mvnw spring-boot:run
```

- **API base:** `http://localhost:8081/api`
- **Swagger:** `http://localhost:8081/swagger-ui.html`

## Default Admin Account

| Field    | Value                |
|----------|----------------------|
| Email    | iradianah5@gmail.com |
| Password | Admin@12345          |

---

## Email Setup (Required for credential & OTP emails)

### Step 1 — Create Gmail App Password

1. Open [Google App Passwords](https://myaccount.google.com/apppasswords)
2. Enable **2-Step Verification** on your Google account first
3. Create an App Password for **Mail**
4. Copy the 16-character password (e.g. `abcd efgh ijkl mnop`)

### Step 2 — Configure local properties

Copy the example file:

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Edit `application-local.properties`:

```properties
spring.mail.username=iradianah5@gmail.com
spring.mail.password=your16charapppassword
app.mail.from=iradianah5@gmail.com
app.mail.enabled=true
```

> **Important:** Remove spaces from the App Password when pasting.

### Step 3 — Restart the app

```bash
./mvnw spring-boot:run
```

---

## Swagger: How to Authorize

1. Call `POST /api/auth/login` with admin credentials
2. Copy `data.accessToken` from the response
3. Click **Authorize** (top right in Swagger)
4. Enter: `Bearer <your-token>`
5. Click **Authorize** → **Close**

Switch users by logging in again and re-authorizing with the new token.

---

## Swagger: Full Test Workflow

### Phase 0 — Verify email works (Admin)

| Step | Endpoint | Body |
|------|----------|------|
| 1 | `POST /api/auth/login` | `{"email":"iradianah5@gmail.com","password":"Admin@12345"}` |
| 2 | Authorize with admin token | — |
| 3 | `POST /api/system/email/test` | `{"to":"mrshhh39@gmail.com"}` |

**Expected:** `emailSent: true` and test email in inbox (check spam).

---

### Phase 1 — Admin setup

| Step | Endpoint | Role | Body example |
|------|----------|------|--------------|
| 4 | `POST /api/tariffs` | Admin | Water FLAT tariff (see below) |
| 5 | `POST /api/tariffs` | Admin | Electricity TIERED tariff |
| 6 | `POST /api/taxes` | Admin | `{"name":"VAT 18%","percentage":18,"effectiveFrom":"2026-01-01"}` |
| 7 | `POST /api/users` | Admin | Create Finance user |

**Water tariff:**
```json
{
  "name": "Water Flat 2026",
  "meterType": "WATER",
  "tariffType": "FLAT",
  "effectiveFrom": "2026-01-01",
  "fixedServiceCharge": 500,
  "unitRate": 350
}
```

**Create Finance user:**
```json
{
  "fullNames": "IRANZI Dianah",
  "email": "mrshhh39@gmail.com",
  "phoneNumber": "+250722500332",
  "roles": ["ROLE_FINANCE"]
}
```

Finance user receives credentials email. If not, use `POST /api/users/{id}/resend-credentials`.

---

### Phase 2 — Finance user first login

| Step | Endpoint | Notes |
|------|----------|-------|
| 8 | `POST /api/auth/login` | Use email + temp password from email/API |
| 9 | `POST /api/auth/change-password` | Required when `mustChangePassword: true` |
| 10 | `POST /api/auth/login` | Login with new password |
| 11 | Re-authorize Swagger | Use Finance token |

**Change password body:**
```json
{
  "currentPassword": "temporary-password-from-email",
  "newPassword": "Finance@12345"
}
```

---

### Phase 3 — Customer & meters (Finance)

| Step | Endpoint | Body |
|------|----------|------|
| 12 | `POST /api/customers` | Customer details (nationalId = 16 digits) |
| 13 | `POST /api/meters` | Assign meter to customer |

**Customer:**
```json
{
  "fullNames": "Jean Uwimana",
  "nationalId": "1199887766554433",
  "email": "jean.uwimana@email.com",
  "phoneNumber": "+250788123456",
  "address": "Kigali, Gasabo District",
  "status": "ACTIVE"
}
```

**Meter:**
```json
{
  "meterNumber": "WTR-001",
  "meterType": "WATER",
  "installationDate": "2026-01-15",
  "status": "ACTIVE",
  "customerId": 1
}
```

---

### Phase 4 — Meter reading (Operator)

| Step | Endpoint | Notes |
|------|----------|-------|
| 14 | `POST /api/users` | Create Operator (Admin) |
| 15 | `POST /api/auth/login` | Login as Operator, authorize |
| 16 | `POST /api/meter-readings` | Capture reading |

**Meter reading:**
```json
{
  "meterId": 1,
  "previousReading": 100,
  "currentReading": 145,
  "readingDate": "2026-06-01"
}
```

---

### Phase 5 — Billing & payment (Finance)

| Step | Endpoint | Notes |
|------|----------|-------|
| 17 | `POST /api/auth/login` | Login as Finance, authorize |
| 18 | `POST /api/bills/generate` | Generate bill for customer + month |
| 19 | `PATCH /api/bills/{id}/approve` | Approve bill |
| 20 | `POST /api/payments` | Record payment |

**Generate bill:**
```json
{
  "customerId": 1,
  "billingMonth": 6,
  "billingYear": 2026
}
```

**Payment:**
```json
{
  "billId": 1,
  "amountPaid": 5000,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-06-05"
}
```

---

## Roles

| Role | Permissions |
|------|-------------|
| ROLE_ADMIN | Tariffs, users, taxes, penalties, full CRUD |
| ROLE_FINANCE | Customers, meters, bills, payments |
| ROLE_OPERATOR | Meter readings |
| ROLE_CUSTOMER | View bills, payments, notifications |

## API Modules

| Module | Base Path |
|--------|-----------|
| Auth | `/api/auth` |
| System | `/api/system` |
| Users | `/api/users` |
| Customers | `/api/customers` |
| Meters | `/api/meters` |
| Meter Readings | `/api/meter-readings` |
| Tariffs | `/api/tariffs` |
| Taxes | `/api/taxes` |
| Penalties | `/api/penalties` |
| Bills | `/api/bills` |
| Payments | `/api/payments` |
| Notifications | `/api/notifications` |

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Email test fails "Authentication failed" | Generate new Gmail App Password, update `application-local.properties`, restart |
| 401 Unauthorized | Login again, re-authorize in Swagger |
| 403 must change password | Call `POST /api/auth/change-password` first |
| Bill generation fails | Ensure operator captured reading for that month |
| Inactive customer | Set status to ACTIVE via `PATCH /api/customers/{id}/status?status=ACTIVE` |
