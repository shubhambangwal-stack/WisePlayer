# WisePlayer Payment API Documentation

This document provides a comprehensive list of all API endpoints related to payments, subscriptions, and revenue management.

---

## 1. Client / Device-Side Endpoints (`/api/payment`)

### Create Checkout Session
`POST /api/payment/checkout`
Initiates a Stripe checkout session for a subscription.

**Request Body:**
```json
{
  "deviceId": "string (MAC/Fingerprint)",
  "plan": "MONTHLY | ANNUAL | LIFETIME | TRIAL"
}
```
**Response:**
```json
{
  "sessionId": "string",
  "url": "string (Stripe Redirect URL)"
}
```

### Get All Invoices
`GET /api/payment/invoices?deviceId={deviceId}`
Retrieves all historical payment invoices for a specific device.

### Get Current Active Invoice
`GET /api/payment/invoice/current?deviceId={deviceId}`
Retrieves the latest successful payment invoice representing the current active subscription.

---

## 2. Reseller / Admin-Side Endpoints (`/api/reseller`)

### Submit Activation Request (Manual Payment)
`POST /api/reseller/activation-request`
Resellers can submit a manual recharge request for a user if they take payment offline.

**Request Body:**
```json
{
  "deviceId": "UUID",
  "planName": "string",
  "notes": "string (Optional)"
}
```

---

## 3. Super-Admin / Admin Management (`/api/admin`)

### List All Payments
`GET /api/admin/payments?page=0&size=20`
Provides a paginated list of all payment attempts in the system.

### Get Payment Statistics
`GET /api/admin/payments/stats`
Retrieves high-level revenue and transaction counts.

### Revenue Reports (New)
`GET /api/admin/reports/revenue?from={ISO_DATE}&to={ISO_DATE}`
Returns a detailed revenue breakdown within a specific date range.

### Manual Subscription Activation
`POST /api/admin/subscriptions/manual`
Directly activates a subscription for a device without needing a payment record.

**Request Body:**
```json
{
  "deviceId": "string",
  "plan": "MONTHLY | ANNUAL | LIFETIME"
}
```

### Revoke Subscription
`DELETE /api/admin/subscriptions/{id}`
Cancels an active subscription immediately.

---

## 4. Webhooks (System-to-System)

### Stripe Webhook
`POST /api/payment/webhook`
Handles Stripe events like `checkout.session.completed`. (Note: Currently disabled in codebase).

### PayPal Webhook
`POST /api/payment/paypal/webhook`
Handles asynchronous payment notifications from PayPal.
