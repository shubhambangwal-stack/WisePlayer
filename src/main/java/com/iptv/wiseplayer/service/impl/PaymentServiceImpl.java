package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Payment;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.dto.request.CheckoutRequest;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.repository.PaymentRepository;
import com.iptv.wiseplayer.service.DeviceService;
import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.service.SubscriptionService;
import com.iptv.wiseplayer.dto.response.SubscriptionResponse;
import com.iptv.wiseplayer.dto.response.InvoiceResponse;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final DeviceService deviceService;
    private final SubscriptionService subscriptionService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Value("${paypal.client-id}")
    private String paypalClientId;

    @Value("${paypal.client-secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode}")
    private String paypalMode;

    @Value("${paypal.return-url:http://localhost:8081/api/payment/paypal/success}")
    private String paypalReturnUrl;

    @Value("${paypal.cancel-url:http://localhost:8081/api/payment/paypal/cancel}")
    private String paypalCancelUrl;

    @Value("${paypal.webhook-id}")
    private String paypalWebhookId;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
            DeviceService deviceService,
            SubscriptionService subscriptionService) {
        this.paymentRepository = paymentRepository;
        this.deviceService = deviceService;
        this.subscriptionService = subscriptionService;
        this.restTemplate = new org.springframework.web.client.RestTemplate();
    }

    private String getPaypalBaseUrl() {
        return "live".equalsIgnoreCase(paypalMode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    private String getAccessToken() {
        String auth = paypalClientId + ":" + paypalClientSecret;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(paypalClientId, paypalClientSecret);

        org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(
                body, headers);

        org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                getPaypalBaseUrl() + "/v1/oauth2/token", request, java.util.Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new RuntimeException("Failed to get PayPal access token");
    }

    @Override
    @Transactional
    public CheckoutResponse createCheckoutSession(CheckoutRequest request) {
        // 1. Check if device already has a PAID subscription
        SubscriptionResponse subStatus = subscriptionService.getSubscriptionStatus(request.getDeviceId());

        // Block if user already has a PAID subscription
        if (subStatus.getType() == SubscriptionType.PAID_ANNUAL
                || subStatus.getType() == SubscriptionType.PAID_LIFETIME) {

            // If it's LIFETIME, they never need to pay again
            if (subStatus.getType() == SubscriptionType.PAID_LIFETIME) {
                log.warn("Checkout blocked for device {}: Already has a LIFETIME subscription", request.getDeviceId());
                throw new IllegalStateException(
                        "You already have a Lifetime subscription. No further purchase is needed.");
            }

            // If it's ANNUAL and still ACTIVE, they shouldn't pay yet
            if (subStatus.getStatus() == SubscriptionStatus.ACTIVE) {
                log.warn("Checkout blocked for device {}: Already has an active ANNUAL subscription expiring at {}",
                        request.getDeviceId(), subStatus.getEndDate());
                throw new IllegalStateException(
                        "You already have an active Annual subscription. Please wait until it expires to renew.");
            }
        }

        // Note: TRIAL users (active or expired) are ALLOWED to proceed to checkout for
        // a paid plan.

        UUID deviceId = deviceService.resolveDeviceId(request.getDeviceId());
        long amountInCents = 0;
        switch (request.getPlan()) {
            case ANNUAL -> amountInCents = 600; // 6.00 EUR
            case LIFETIME -> amountInCents = 1000; // 10.00 EUR
        }
        BigDecimal amount = BigDecimal.valueOf(amountInCents).divide(BigDecimal.valueOf(100));

        String accessToken = getAccessToken();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        java.util.Map<String, Object> orderRequest = new java.util.HashMap<>();
        orderRequest.put("intent", "CAPTURE");

        java.util.Map<String, Object> purchaseUnit = new java.util.HashMap<>();
        // Create the PENDING payment record BEFORE the API call
        // This ensures the record exists even if the redirect/webhook happens extremely
        // fast
        Payment payment = new Payment();
        payment.setDeviceId(deviceId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setPlan(request.getPlan());
        payment = paymentRepository.save(payment);

        purchaseUnit.put("reference_id", payment.getId().toString()); // Use our internal ID as reference

        java.util.Map<String, Object> amountMap = new java.util.HashMap<>();
        amountMap.put("currency_code", "EUR");
        amountMap.put("value", amount.toString());
        purchaseUnit.put("amount", amountMap);

        orderRequest.put("purchase_units", java.util.Collections.singletonList(purchaseUnit));

        java.util.Map<String, String> applicationContext = new java.util.HashMap<>();
        applicationContext.put("return_url", paypalReturnUrl);
        applicationContext.put("cancel_url", paypalCancelUrl);
        applicationContext.put("landing_page", "BILLING");
        applicationContext.put("user_action", "PAY_NOW");
        applicationContext.put("shipping_preference", "NO_SHIPPING");
        orderRequest.put("application_context", applicationContext);

        org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(
                orderRequest, headers);

        org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                getPaypalBaseUrl() + "/v2/checkout/orders", entity, java.util.Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String orderId = (String) response.getBody().get("id");
            String approveUrl = "";

            java.util.List<java.util.Map<String, String>> links = (java.util.List<java.util.Map<String, String>>) response
                    .getBody().get("links");
            for (java.util.Map<String, String> link : links) {
                if ("approve".equals(link.get("rel"))) {
                    approveUrl = link.get("href");
                }
            }

            // Update the record with the PayPal Order ID
            payment.setPaypalOrderId(orderId);
            paymentRepository.save(payment);

            log.info("PayPal Order {} created and linked to Payment ID {}", orderId, payment.getId());

            return new CheckoutResponse(approveUrl, orderId);
        }

        // If it failed, we might want to delete or mark the payment as FAILED
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        throw new RuntimeException("Failed to create PayPal order");
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        // Stripe webhook logic commented out
        /*
         * log.info("Received Stripe webhook...");
         * ...
         */
        log.info("Stripe webhook received but ignored as Stripe is disabled.");
    }

    @Override
    @Transactional
    public void handlePaypalWebhook(java.util.Map<String, Object> payload, java.util.Map<String, String> headers) {
        if (!verifyWebhookSignature(payload, headers)) {
            log.error("Invalid PayPal Webhook signature detected!");
            throw new RuntimeException("Invalid PayPal Webhook signature");
        }

        String eventType = (String) payload.get("event_type");
        log.info("Received PayPal webhook: {}", eventType);

        switch (eventType) {
            case "PAYMENT.CAPTURE.COMPLETED" -> processCaptureCompleted(payload);
            case "PAYMENT.CAPTURE.DENIED" -> processCaptureDenied(payload);
            case "CHECKOUT.ORDER.APPROVED" -> processCheckoutOrderApproved(payload);
            case "PAYMENT.CAPTURE.REFUNDED", "PAYMENT.CAPTURE.REVERSED" -> processRefundOrReversal(payload);
            case "CUSTOMER.DISPUTE.CREATED" -> processDisputeCreated(payload);
            case "PAYMENT.CAPTURE.PENDING" -> processPaymentPending(payload);
            default -> log.info("Ignored PayPal event type: {}", eventType);
        }
    }

    private void processSuccessfulPaypalPayment(String orderId, String captureId, BigDecimal fee) {
        log.info("Processing successful PayPal payment for Order ID: {}", orderId);
        Payment payment = paymentRepository.findByPaypalOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("CRITICAL: Payment record not found for PayPal Order ID: {}", orderId);
                    return new RuntimeException("Payment record not found for Order ID: " + orderId);
                });

        // Idempotency check
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment for Order ID: {} is already marked as SUCCESS. Skipping activation.", orderId);
            return;
        }

        if (captureId != null) {
            payment.setPaypalCaptureId(captureId);
        }
        if (fee != null) {
            payment.setPaypalFee(fee);
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        SubscriptionActivationRequest activationRequest = new SubscriptionActivationRequest();
        activationRequest.setDeviceId(payment.getDeviceId().toString());
        activationRequest.setPlan(payment.getPlan());
        subscriptionService.activateSubscription(activationRequest);

        log.info("PayPal Subscription activated successfully for device: {}", payment.getDeviceId());
    }

    private void processCaptureDenied(java.util.Map<String, Object> payload) {
        String orderId = extractOrderId(payload);
        if (orderId != null) {
            log.warn("Payment capture DENIED for Order ID: {}", orderId);
            paymentRepository.findByPaypalOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            });
        } else {
            log.error("Received PAYMENT.CAPTURE.DENIED but could not extract Order ID.");
        }
    }

    private void processCheckoutOrderApproved(java.util.Map<String, Object> payload) {
        java.util.Map<String, Object> resource = (java.util.Map<String, Object>) payload.get("resource");
        String orderId = (String) resource.get("id");

        if (orderId != null) {
            paymentRepository.findByPaypalOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    log.info("Auto-capturing pending order: {}", orderId);
                    try {
                        captureOrder(orderId);
                    } catch (Exception e) {
                        log.error("Failed to auto-capture order: {}", orderId, e);
                    }
                }
            });
        }

    }

    @Override
    @Transactional
    public void captureOrder(String orderId) {
        if (orderId == null) {
            log.error("Capture failed: Order ID is null");
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        final String finalOrderId = orderId.trim();
        log.info("Attempting to capture PayPal order: {}", finalOrderId);

        // 1. Check if payment exists first to avoid unnecessary API calls or cryptic
        // errors
        // Use a small retry loop to handle transaction commit lag from the creation
        // step
        Payment payment = null;
        int maxRetries = 2;
        for (int i = 0; i < maxRetries; i++) {
            payment = paymentRepository.findByPaypalOrderId(finalOrderId).orElse(null);
            if (payment != null)
                break;

            if (i < maxRetries - 1) {
                log.warn("Payment record not found for Order ID: {} on attempt {}. Retrying in 500ms...", finalOrderId,
                        i + 1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (payment == null) {
            log.error("Cannot capture order: Payment record not found for Order ID: {} after retries", finalOrderId);
            throw new RuntimeException("Payment record not found for Order ID: " + finalOrderId);
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Order {} already captured and processed successfully.", orderId);
            return;
        }

        String accessToken = getAccessToken();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("{}", headers);

        try {
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    getPaypalBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture", entity, java.util.Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("PayPal Order captured successfully API call: {}", orderId);

                String captureId = null;
                BigDecimal fee = null;

                try {
                    java.util.List<java.util.Map<String, Object>> purchaseUnits = (java.util.List<java.util.Map<String, Object>>) response
                            .getBody().get("purchase_units");

                    if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                        java.util.Map<String, Object> payments = (java.util.Map<String, Object>) purchaseUnits.get(0)
                                .get("payments");
                        if (payments != null) {
                            java.util.List<java.util.Map<String, Object>> captures = (java.util.List<java.util.Map<String, Object>>) payments
                                    .get("captures");

                            if (captures != null && !captures.isEmpty()) {
                                java.util.Map<String, Object> capture = captures.get(0);
                                captureId = (String) capture.get("id");

                                java.util.Map<String, Object> sellerReceivableBreakdown = (java.util.Map<String, Object>) capture
                                        .get("seller_receivable_breakdown");
                                if (sellerReceivableBreakdown != null) {
                                    java.util.Map<String, Object> paypalFeeMap = (java.util.Map<String, Object>) sellerReceivableBreakdown
                                            .get("paypal_fee");
                                    if (paypalFeeMap != null) {
                                        String feeValue = (String) paypalFeeMap.get("value");
                                        fee = new BigDecimal(feeValue);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to extract capture details from response for Order ID: {}", orderId, e);
                }

                processSuccessfulPaypalPayment(orderId, captureId, fee);
            } else {
                log.error("PayPal capture API returned non-success status: {}. Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("PayPal capture failed with status: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("PayPal API Error during capture! Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("PayPal API capture error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error capturing PayPal order: {}", orderId, e);
            throw new RuntimeException("Unexpected error during PayPal capture", e);
        }
    }

    private void processRefundOrReversal(java.util.Map<String, Object> payload) {
        String eventType = (String) payload.get("event_type");
        log.warn("Processing {} ...", eventType);

        String orderId = extractOrderId(payload);
        if (orderId == null) {
            log.error("Could not link {} to an Order ID.", eventType);
            return;
        }

        paymentRepository.findByPaypalOrderId(orderId).ifPresent(payment -> {
            PaymentStatus newStatus = eventType.contains("REFUNDED")
                    ? PaymentStatus.REFUNDED
                    : PaymentStatus.REVERSED;

            log.warn("Marking Order ID: {} as {}. Revoking subscription...", orderId, newStatus);

            payment.setStatus(newStatus);
            paymentRepository.save(payment);

            subscriptionService.revokeSubscription(payment.getDeviceId().toString());
        });
    }

    private void processDisputeCreated(java.util.Map<String, Object> payload) {
        log.warn("Processing CUSTOMER.DISPUTE.CREATED ...");
        // Disputes are tricky to link back to Order ID directly via standard
        // supplementary_data sometimes.
        // But let's try the standard extraction first.
        String orderId = extractOrderId(payload);

        if (orderId == null) {
            // Fallback: Try to find by Capture ID if available in disputed_transactions
            java.util.Map<String, Object> resource = (java.util.Map<String, Object>) payload.get("resource");
            java.util.List<java.util.Map<String, Object>> disputedTransactions = (java.util.List<java.util.Map<String, Object>>) resource
                    .get("disputed_transactions");

            if (disputedTransactions != null && !disputedTransactions.isEmpty()) {
                String captureId = (String) disputedTransactions.get(0).get("buyer_transaction_id"); // often maps to
                                                                                                     // capture id // or
                                                                                                     // seller_transaction_id
                if (captureId == null)
                    captureId = (String) disputedTransactions.get(0).get("seller_transaction_id");

                if (captureId != null) {
                    log.info("Found Capture ID {} in dispute. Searching payment...", captureId);
                    // We don't have findByCaptureId yet, but we can assume we might need it.
                    // For now, if we can't find it, we log error.
                    // TO DO: Add findByPaypalCaptureId to repo if needed.
                }
            }
            log.error("Could not link DISPUTE to an Order ID. Manual intervention required.");
            return;
        }

        paymentRepository.findByPaypalOrderId(orderId).ifPresent(payment -> {
            log.warn("Dispute created for Order ID: {}. Revoking subscription and marking DISPUTED.", orderId);
            payment.setStatus(PaymentStatus.DISPUTED);
            paymentRepository.save(payment);
            subscriptionService.revokeSubscription(payment.getDeviceId().toString());
        });
    }

    private void processPaymentPending(java.util.Map<String, Object> payload) {
        String orderId = extractOrderId(payload);
        if (orderId != null) {
            log.info("Payment for Order ID: {} is PENDING. Waiting for completion...", orderId);
        } else {
            log.info("Received PAYMENT.CAPTURE.PENDING but could not extract Order ID.");
        }
    }

    private String extractOrderId(java.util.Map<String, Object> payload) {
        java.util.Map<String, Object> resource = (java.util.Map<String, Object>) payload.get("resource");
        if (resource == null)
            return null;

        java.util.Map<String, Object> supplementaryData = (java.util.Map<String, Object>) resource
                .get("supplementary_data");
        if (supplementaryData != null) {
            java.util.Map<String, Object> relatedIds = (java.util.Map<String, Object>) supplementaryData
                    .get("related_ids");
            if (relatedIds != null) {
                return (String) relatedIds.get("order_id");
            }
        }
        return null; // Could not extract
    }

    private void processCaptureCompleted(java.util.Map<String, Object> payload) {
        String orderId = extractOrderId(payload);
        if (orderId == null) {
            log.error("Could not determine Order ID from PayPal Webhook for PAYMENT.CAPTURE.COMPLETED: {}", payload);
            return;
        }

        java.util.Map<String, Object> resource = (java.util.Map<String, Object>) payload.get("resource");
        String captureId = (String) resource.get("id");

        // Extract Fee
        BigDecimal fee = BigDecimal.ZERO;
        java.util.Map<String, Object> sellerReceivableBreakdown = (java.util.Map<String, Object>) resource
                .get("seller_receivable_breakdown");
        if (sellerReceivableBreakdown != null) {
            java.util.Map<String, Object> paypalFeeMap = (java.util.Map<String, Object>) sellerReceivableBreakdown
                    .get("paypal_fee");
            if (paypalFeeMap != null) {
                String feeValue = (String) paypalFeeMap.get("value");
                fee = new BigDecimal(feeValue);
            }
        }

        log.info("Extracted Order ID {}, Capture ID {}, Fee {} from capture webhook", orderId, captureId, fee);
        processSuccessfulPaypalPayment(orderId, captureId, fee);
    }

    private boolean verifyWebhookSignature(java.util.Map<String, Object> payload,
            java.util.Map<String, String> headers) {
        try {
            String accessToken = getAccessToken();
            org.springframework.http.HttpHeaders authHeaders = new org.springframework.http.HttpHeaders();
            authHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            authHeaders.setBearerAuth(accessToken);

            java.util.Map<String, Object> verificationRequest = new java.util.HashMap<>();
            verificationRequest.put("auth_algo", headers.get("paypal-auth-algo"));
            verificationRequest.put("cert_url", headers.get("paypal-cert-url"));
            verificationRequest.put("transmission_id", headers.get("paypal-transmission-id"));
            verificationRequest.put("transmission_sig", headers.get("paypal-transmission-sig"));
            verificationRequest.put("transmission_time", headers.get("paypal-transmission-time"));
            verificationRequest.put("webhook_id", paypalWebhookId);
            verificationRequest.put("webhook_event", payload);

            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(
                    verificationRequest, authHeaders);

            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    getPaypalBaseUrl() + "/v1/notifications/verify-webhook-signature", entity, java.util.Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String verificationStatus = (String) response.getBody().get("verification_status");
                return "SUCCESS".equalsIgnoreCase(verificationStatus);
            }
        } catch (Exception e) {
            log.error("Webhook signature verification failed due to error", e);
        }
        return false;
    }

    @Override
    public java.util.List<com.iptv.wiseplayer.dto.response.InvoiceResponse> getAllInvoicesByDevice(String deviceId) {
        log.info("Fetching all invoices for device: {}", deviceId);
        UUID resolvedDeviceId = deviceService.resolveDeviceId(deviceId);

        java.util.List<Payment> payments = paymentRepository.findAllByDeviceIdOrderByCreatedAtDesc(resolvedDeviceId);

        return payments.stream()
                .map(this::mapToInvoiceResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public InvoiceResponse getCurrentInvoice(String deviceId) {
        log.info("Fetching current invoice for device: {}", deviceId);
        UUID resolvedDeviceId = deviceService.resolveDeviceId(deviceId);

        return paymentRepository
                .findTopByDeviceIdAndStatusOrderByCreatedAtDesc(resolvedDeviceId, PaymentStatus.SUCCESS)
                .map(this::mapToInvoiceResponse)
                .orElse(null);
    }

    private com.iptv.wiseplayer.dto.response.InvoiceResponse mapToInvoiceResponse(Payment payment) {
        com.iptv.wiseplayer.dto.response.InvoiceResponse response = new com.iptv.wiseplayer.dto.response.InvoiceResponse();
        response.setInvoiceNumber("INV-" + payment.getId().toString().substring(0, 8).toUpperCase());
        response.setPaymentId(payment.getId());
        response.setDeviceId(payment.getDeviceId());
        response.setTransactionDate(payment.getCreatedAt());
        response.setStatus(payment.getStatus());
        response.setPlan(payment.getPlan());
        response.setPlanDisplayName(getPlanDisplayName(payment.getPlan()));
        response.setAmount(payment.getAmount());
        response.setCurrency("EUR");
        response.setPaymentMethod("PayPal");
        response.setPaypalOrderId(payment.getPaypalOrderId());
        response.setPaypalCaptureId(payment.getPaypalCaptureId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }

    private String getPlanDisplayName(SubscriptionPlan plan) {
        return switch (plan) {
            case ANNUAL -> "Annual Subscription";
            case LIFETIME -> "Lifetime Subscription";
            default -> "Unknown Plan";
        };
    }
}
