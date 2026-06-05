# Swagger UI Guide — Utility Billing System

Use this guide to test the entire system through Swagger. No Postman or frontend is required.

**Swagger URL:** `http://localhost:8081/swagger-ui.html`  
**API base:** `http://localhost:8081/api`

---

## Table of contents

1. [Before you start](#1-before-you-start)
2. [Swagger basics](#2-swagger-basics)
3. [How to login and authorize](#3-how-to-login-and-authorize)
4. [Switch between roles](#4-switch-between-roles)
5. [Full test workflow (step by step)](#5-full-test-workflow-step-by-step)
6. [Swagger tags explained](#6-swagger-tags-explained)
7. [Sample request bodies](#7-sample-request-bodies)
8. [Common errors and fixes](#8-common-errors-and-fixes)
9. [Quick reference checklist](#9-quick-reference-checklist)

---

## 1. Before you start

Make sure these are running:

| Requirement | How to check |
|-------------|--------------|
| App running | Open `http://localhost:8081/swagger-ui.html` — page loads |
| PostgreSQL | Database `utility_billing_db` exists |
| Email (optional) | `application-local.properties` has Gmail App Password |

**Default admin account:**

| Field | Value |
|-------|-------|
| Email | `iradianah5@gmail.com` |
| Password | `Admin@12345` |

---

## 2. Swagger basics

### What you see in Swagger

- **Tags** (left side) — groups of endpoints (Authentication, Customers, Bills, etc.)
- **Lock icon** on an endpoint — requires JWT token (most endpoints)
- **No lock** — public endpoint (login, register)
- **Try it out** — enables the form to send a request
- **Execute** — sends the request to the server
- **Authorize** (green button, top right) — paste your JWT token here

### How to call any endpoint

1. Expand the endpoint (e.g. `POST /api/customers`)
2. Click **Try it out**
3. Fill in the **Request body** or **Parameters**
4. Click **Execute**
5. Read the **Response body** below (status code + JSON)

### Understanding responses

Every response looks like this:

```json
{
  "success": true,
  "message": "Customer created successfully",
  "data": { ... }
}
```

- `success: true` → request worked
- `data` → the actual result (customer, bill, token, etc.)
- `success: false` or status **400/401/403/500** → something failed (read `message`)

---

## 3. How to login and authorize

This is the **most important step**. Almost every endpoint needs a token.

### Step 1 — Login (no token needed yet)

1. Open tag **Authentication**
2. Open `POST /api/auth/login`
3. Click **Try it out**
4. Paste:

```json
{
  "email": "iradianah5@gmail.com",
  "password": "Admin@12345"
}
```

5. Click **Execute**
6. In the response, copy `data.accessToken` (long string starting with `eyJ...`)

### Step 2 — Authorize Swagger

1. Click the green **Authorize** button (top right)
2. In the **Value** field, type:

```
Bearer eyJhbGciOiJIUzUxMiJ9...
```

Replace `eyJ...` with your actual `accessToken`. **Include the word `Bearer` and a space.**

3. Click **Authorize**
4. Click **Close**

You are now logged in as Admin. All locked endpoints will send this token automatically.

### Step 3 — Logout (optional)

**Authentication → `POST /api/auth/logout`** — blacklists the current token.

---

## 4. Switch between roles

Different users have different permissions. To test as Finance, Operator, or Customer:

1. **Authentication → `POST /api/auth/login`** — login as that user
2. Copy the new `accessToken`
3. Click **Authorize** again — paste the new `Bearer <token>`
4. Now Swagger acts as that user

| Role | Used for |
|------|----------|
| **Admin** | Tariffs, taxes, users, everything |
| **Finance** | Customers, meters, bills, payments |
| **Operator** | Meter readings only |
| **Customer** | View own bills and notifications |

### First login for staff (Finance / Operator)

When admin creates a user via `POST /api/users`, they get a **temporary password** by email (or from `resend-credentials` response).

1. Login with temporary password → response shows `"mustChangePassword": true`
2. You can **only** use these endpoints until password is changed:
   - `POST /api/auth/change-password`
   - `POST /api/auth/logout`
   - `POST /api/auth/refresh`
3. Call **change-password**:

```json
{
  "currentPassword": "temporary-password-here",
  "newPassword": "Finance@12345"
}
```

4. Login again with the new password
5. Re-authorize in Swagger with the new token

---

## 5. Full test workflow (step by step)

Follow this order — each step depends on the previous one.

### Phase 0 — Verify email (Admin)

| Step | Tag | Endpoint | Body |
|------|-----|----------|------|
| 0.1 | Authentication | `POST /api/auth/login` | Admin credentials |
| 0.2 | — | **Authorize** with admin token | — |
| 0.3 | System | `POST /api/system/email/test` | `{"to":"your-email@gmail.com"}` |

If `data.sent: true`, email is working.

---

### Phase 1 — Admin setup (one time)

| Step | Tag | Endpoint | Who |
|------|-----|----------|-----|
| 1.1 | Tariffs | `POST /api/tariffs` | Admin — water tariff |
| 1.2 | Tariffs | `POST /api/tariffs` | Admin — electricity tariff |
| 1.3 | Taxes | `POST /api/taxes` | Admin — VAT |
| 1.4 | Penalties | `POST /api/penalties` | Admin — optional |
| 1.5 | Users | `POST /api/users` | Admin — create Finance user |
| 1.6 | Users | `POST /api/users` | Admin — create Operator user |

**Save IDs** from responses (tariff id, user id, etc.).

If Finance user did not get email: **Users → `POST /api/users/{id}/resend-credentials`**

---

### Phase 2 — Finance: customers and meters

| Step | Tag | Endpoint | Notes |
|------|-----|----------|-------|
| 2.1 | Authentication | `POST /api/auth/login` | Finance user |
| 2.2 | — | **Authorize** with Finance token | — |
| 2.3 | Customers | `POST /api/customers` | Note `data.id` (e.g. `1`) |
| 2.4 | Meters | `POST /api/meters` | Use `customerId` from 2.3 |
| 2.5 | Customers | `GET /api/customers` | Verify customer appears |

---

### Phase 3 — Operator: meter reading

| Step | Tag | Endpoint | Notes |
|------|-----|----------|-------|
| 3.1 | Authentication | `POST /api/auth/login` | Operator user |
| 3.2 | — | **Authorize** with Operator token | — |
| 3.3 | Meter Readings | `POST /api/meter-readings` | Use `meterId` from Phase 2 |
| 3.4 | Meter Readings | `GET /api/meter-readings` | Verify reading saved |

---

### Phase 4 — Finance: billing and payment

| Step | Tag | Endpoint | Notes |
|------|-----|----------|-------|
| 4.1 | Authentication | `POST /api/auth/login` | Finance user |
| 4.2 | — | **Authorize** with Finance token | — |
| 4.3 | Bills | `POST /api/bills/generate` | Use `customerId`, month, year |
| 4.4 | Bills | `PATCH /api/bills/{id}/approve` | Use bill `id` from 4.3 |
| 4.5 | Payments | `POST /api/payments` | Use `billId` from 4.3 |
| 4.6 | Bills | `GET /api/bills/{id}` | Check status PAID or partial |
| 4.7 | Notifications | `GET /api/notifications/customer/{customerId}` | Bill notification |

---

### Phase 5 — Verify (optional)

| Step | Tag | Endpoint |
|------|-----|----------|
| 5.1 | Customers | `GET /api/customers/search?query=Jean` |
| 5.2 | Bills | `GET /api/bills/customer/{customerId}` |
| 5.3 | Payments | `GET /api/payments/bill/{billId}` |

---

## 6. Swagger tags explained

| Tag | What it does | Who can use it |
|-----|--------------|----------------|
| **Authentication** | Login, register, change password, OTP | Everyone (login is public) |
| **System** | Test email configuration | Admin |
| **Users** | Create staff, resend credentials, assign roles | Admin |
| **Customers** | Create/update customers, search, filter by status | Admin, Finance (create); all staff (view) |
| **Meters** | Assign water/electricity meters to customers | Admin, Operator (create); Finance (view) |
| **Meter Readings** | Capture consumption readings | Operator (create); others (view) |
| **Tariffs** | Set water/electricity rates (flat or tiered) | Admin (create); Finance (view) |
| **Taxes** | Configure tax percentage | Admin |
| **Penalties** | Late payment penalty rules | Admin |
| **Bills** | Generate monthly bill, approve, view | Finance (generate/approve); Customer (view own) |
| **Payments** | Record partial or full payment | Finance (record); Customer (view) |
| **Notifications** | Customer alerts (bill ready, payment received) | Customer, Admin, Finance |
| **Files** | Upload/download documents | Authenticated users |

---

## 7. Sample request bodies

Copy these into Swagger **Request body** fields.

### Login (Admin)

```json
{
  "email": "iradianah5@gmail.com",
  "password": "Admin@12345"
}
```

### Create Finance user (Admin)

```json
{
  "fullNames": "IRANZI Dianah",
  "email": "mrshhh39@gmail.com",
  "phoneNumber": "+250722500332",
  "roles": ["ROLE_FINANCE"]
}
```

### Create Operator user (Admin)

```json
{
  "fullNames": "John Operator",
  "email": "operator@email.com",
  "phoneNumber": "+250788123456",
  "roles": ["ROLE_OPERATOR"]
}
```

### Water tariff — FLAT (Admin)

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

### Electricity tariff — TIERED (Admin)

```json
{
  "name": "Electricity Tiered 2026",
  "meterType": "ELECTRICITY",
  "tariffType": "TIERED",
  "effectiveFrom": "2026-01-01",
  "fixedServiceCharge": 1000,
  "tiers": [
    { "minUnits": 0, "maxUnits": 50, "ratePerUnit": 120 },
    { "minUnits": 51, "maxUnits": null, "ratePerUnit": 180 }
  ]
}
```

### Tax (Admin)

```json
{
  "name": "VAT 18%",
  "percentage": 18,
  "effectiveFrom": "2026-01-01"
}
```

### Customer (Finance)

```json
{
  "fullNames": "Jean Uwimana",
  "nationalId": "1199887766554433",
  "email": "jean.uwimana@email.com",
  "phoneNumber": "+250788123456",
  "address": "Kigali, Gasabo District, Rwanda",
  "status": "ACTIVE"
}
```

> `nationalId` must be exactly **16 digits** and **unique**.

### Meter (Finance or Admin)

```json
{
  "meterNumber": "WTR-001",
  "meterType": "WATER",
  "installationDate": "2026-01-15",
  "status": "ACTIVE",
  "customerId": 1
}
```

`meterType`: `WATER` or `ELECTRICITY`

### Meter reading (Operator)

```json
{
  "meterId": 1,
  "previousReading": 100,
  "currentReading": 145,
  "readingDate": "2026-06-01"
}
```

> `currentReading` must be **≥ previousReading**.  
> `readingDate` month/year must match the bill you will generate.

### Generate bill (Finance)

```json
{
  "customerId": 1,
  "billingMonth": 6,
  "billingYear": 2026
}
```

### Record payment (Finance)

```json
{
  "billId": 1,
  "amountPaid": 5000,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-06-05"
}
```

`paymentMethod`: `CASH`, `BANK_TRANSFER`, `MOBILE_MONEY`, `CARD`

### Change password (staff first login)

```json
{
  "currentPassword": "temporary-password-from-email",
  "newPassword": "Finance@12345"
}
```

### Test email (Admin)

```json
{
  "to": "mrshhh39@gmail.com"
}
```

---

## 8. Common errors and fixes

| Status | Message | Cause | Fix |
|--------|---------|-------|-----|
| **401** | Unauthorized | No token or expired token | Login again → Authorize |
| **403** | Access denied | Wrong role for endpoint | Login as correct role |
| **403** | must change password | Staff first login | Call `change-password` first |
| **400** | National ID already exists | Duplicate customer | Use different `nationalId` |
| **400** | No meter readings found | No reading for billing month | Operator captures reading first |
| **400** | Inactive customers cannot receive bills | Customer is INACTIVE | `PATCH /api/customers/{id}/status?status=ACTIVE` |
| **400** | Bill already exists | Duplicate bill for same month | Use different month or delete bill (Admin) |
| **400** | Gmail SMTP failed | Bad App Password | Update `application-local.properties`, restart |

### Pagination in Swagger

For `GET` endpoints with pagination, you may see parameters like `page`, `size`, `sort`.

- Leave defaults or set `page=0`, `size=10`
- For sort use: `createdAt,desc` (not a JSON array)

---

## 9. Quick reference checklist

Print or follow this list when demoing the system:

```
[ ] Start app — Swagger opens at localhost:8081/swagger-ui.html
[ ] POST /api/auth/login (Admin)
[ ] Authorize with Bearer token
[ ] POST /api/system/email/test
[ ] POST /api/tariffs (Water)
[ ] POST /api/tariffs (Electricity)
[ ] POST /api/taxes
[ ] POST /api/users (Finance)
[ ] POST /api/users (Operator)
[ ] POST /api/auth/login (Finance) + change-password if needed
[ ] Authorize as Finance
[ ] POST /api/customers
[ ] POST /api/meters
[ ] POST /api/auth/login (Operator)
[ ] Authorize as Operator
[ ] POST /api/meter-readings
[ ] POST /api/auth/login (Finance)
[ ] Authorize as Finance
[ ] POST /api/bills/generate
[ ] PATCH /api/bills/{id}/approve
[ ] POST /api/payments
[ ] GET /api/bills/{id} — verify PAID / balance
```

---

## System logic (what happens behind Swagger)

```
Admin sets tariffs & tax
        ↓
Admin creates Finance + Operator users (email with temp password)
        ↓
Finance creates Customer (ACTIVE, unique nationalId)
        ↓
Finance assigns Meter(s) to Customer
        ↓
Operator captures Meter Reading (consumption = current − previous)
        ↓
Finance generates Bill (tariff + tax applied) → status PENDING
        ↓
Finance approves Bill → status APPROVED
        ↓
Finance records Payment → bill PAID or partial balance
        ↓
Customer receives Notification (database trigger)
```

---

## Tips for exam demo

1. **Always Authorize** after every login — Swagger does not remember roles automatically.
2. **Write down IDs** — customer id, meter id, bill id as you create them.
3. **Use the correct endpoint** — staff users: `POST /api/users` (not `/api/auth/register`).
4. **Test email first** — `POST /api/system/email/test` before creating users.
5. **Reading date matters** — bill month must match the meter reading month.

---

*For email setup, see `README.md`. For project overview, see the main README.*
