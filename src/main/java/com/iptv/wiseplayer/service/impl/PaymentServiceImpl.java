package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Payments;
import com.iptv.wiseplayer.domain.entity.SubscriptionPlanConfig;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.dto.request.CheckoutRequest;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.dto.response.InvoiceResponse;
import com.iptv.wiseplayer.dto.response.SubscriptionResponse;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.PaymentRepository;
import com.iptv.wiseplayer.repository.PlanConfigRepository;
import com.iptv.wiseplayer.service.DeviceService;
import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Element;

/**
 * PaymentServiceImpl — plan details are resolved from
 * subscription_plan_configs.
 * The sentinel String "CREDITS" is used for reseller credit top-up payments (no
 * plan lookup).
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String CREDITS_PLAN = "CREDITS";

    private final PaymentRepository paymentRepository;
    private final DeviceService deviceService;
    private final SubscriptionService subscriptionService;
    private final PlanConfigRepository planConfigRepository;

    @Value("${paypal.client-id}")
    private String paypalClientId;

    @Value("${paypal.client-secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode}")
    private String paypalMode;

    @Value("${paypal.return-url:https://api.wise-player.com/api/payment/paypal/success}")
    private String paypalReturnUrl;

    @Value("${paypal.cancel-url:https://api.wise-player.com/api/payment/paypal/cancel}")
    private String paypalCancelUrl;

    @Value("${paypal.webhook-id}")
    private String paypalWebhookId;

    private final com.iptv.wiseplayer.service.CreditService creditService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
            DeviceService deviceService,
            SubscriptionService subscriptionService,
            PlanConfigRepository planConfigRepository,
            com.iptv.wiseplayer.service.CreditService creditService,
            org.springframework.web.client.RestTemplate restTemplate) {
        this.paymentRepository = paymentRepository;
        this.deviceService = deviceService;
        this.subscriptionService = subscriptionService;
        this.planConfigRepository = planConfigRepository;
        this.creditService = creditService;
        this.restTemplate = restTemplate;
    }

    private String getPaypalBaseUrl() {
        return "live".equalsIgnoreCase(paypalMode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    private String getAccessToken() {
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
        return createCheckoutSession(request, null);
    }

    @Override
    @Transactional
    public CheckoutResponse createCheckoutSession(CheckoutRequest request, String customReturnUrl) {
        // 1. Check if device already has a PAID subscription
        SubscriptionResponse subStatus = subscriptionService.getSubscriptionStatus(request.getDeviceId());

        if (subStatus.getType() == SubscriptionType.PAID_ANNUAL
                || subStatus.getType() == SubscriptionType.PAID_LIFETIME) {

            if (subStatus.getType() == SubscriptionType.PAID_LIFETIME) {
                log.warn("Checkout blocked for device {}: Already has a LIFETIME subscription", request.getDeviceId());
                throw new IllegalStateException(
                        "You already have a Lifetime subscription. No further purchase is needed.");
            }

            if (subStatus.getStatus() == SubscriptionStatus.ACTIVE) {
                log.warn("Checkout blocked for device {}: Already has an active ANNUAL subscription expiring at {}",
                        request.getDeviceId(), subStatus.getEndDate());
                throw new IllegalStateException(
                        "You already have an active Annual subscription. Please wait until it expires to renew.");
            }
        }

        // 2. Resolve plan config from DB — gets price dynamically
        SubscriptionPlanConfig planConfig = planConfigRepository.findByName(request.getPlanName())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanName()));

        UUID deviceId = deviceService.resolveDeviceId(request.getDeviceId());
        BigDecimal amount = planConfig.getPrice();

        String accessToken = getAccessToken();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        java.util.Map<String, Object> orderRequest = new java.util.HashMap<>();
        orderRequest.put("intent", "CAPTURE");

        java.util.Map<String, Object> purchaseUnit = new java.util.HashMap<>();

        // Create the PENDING payment record BEFORE the API call
        Payments payment = new Payments();
        payment.setDeviceId(deviceId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setPlanName(planConfig.getName());
        payment.setCurrency(planConfig.getCurrency());
        payment = paymentRepository.save(payment);

        purchaseUnit.put("reference_id", payment.getId().toString());

        java.util.Map<String, Object> amountMap = new java.util.HashMap<>();
        amountMap.put("currency_code", planConfig.getCurrency());
        amountMap.put("value", amount.toString());
        purchaseUnit.put("amount", amountMap);

        orderRequest.put("purchase_units", java.util.Collections.singletonList(purchaseUnit));

        java.util.Map<String, String> applicationContext = new java.util.HashMap<>();
        applicationContext.put("return_url", customReturnUrl != null ? customReturnUrl : paypalReturnUrl);
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

            payment.setPaypalOrderId(orderId);
            paymentRepository.save(payment);

            log.info("PayPal Order {} created and linked to Payment ID {}", orderId, payment.getId());
            return new CheckoutResponse(approveUrl, orderId);
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        throw new RuntimeException("Failed to create PayPal order");
    }

    @Override
    @Transactional
    public CheckoutResponse createCreditCheckoutSession(UUID resellerId, int creditAmount) {
        if (creditAmount >= 1000) {
            int bonus = 200;
            int total = creditAmount + bonus;
            String message = String.format(
                    "For a bulk purchase of %d codes, you are eligible for %d bonus codes (Total: %d). " +
                            "To complete this transaction and claim your bonus, please contact us through our WhatsApp channel.",
                    creditAmount, bonus, total);
            log.warn("Bulk credit purchase of {} codes blocked for reseller {}. Directing to WhatsApp.", creditAmount,
                    resellerId);
            throw new com.iptv.wiseplayer.exception.BadRequestException(message);
        }

        BigDecimal unitPrice = creditService.calculateUnitPrice(creditAmount);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(creditAmount));

        String accessToken = getAccessToken();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        java.util.Map<String, Object> orderRequest = new java.util.HashMap<>();
        orderRequest.put("intent", "CAPTURE");

        java.util.Map<String, Object> purchaseUnit = new java.util.HashMap<>();

        Payments payment = new Payments();
        payment.setResellerId(resellerId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(totalAmount);
        payment.setPlanName(CREDITS_PLAN); // sentinel — no plan config entry needed
        payment.setCreditAmount(creditAmount);
        payment.setCurrency("EUR");
        payment = paymentRepository.save(payment);

        purchaseUnit.put("reference_id", payment.getId().toString());

        java.util.Map<String, Object> amountMap = new java.util.HashMap<>();
        amountMap.put("currency_code", "EUR");
        amountMap.put("value", totalAmount.toString());
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

            payment.setPaypalOrderId(orderId);
            paymentRepository.save(payment);

            log.info("PayPal Credit Order {} created for reseller {}", orderId, resellerId);
            return new CheckoutResponse(approveUrl, orderId);
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        throw new RuntimeException("Failed to create PayPal order for credits");
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
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
        Payments payment = paymentRepository.findByPaypalOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("CRITICAL: Payment record not found for PayPal Order ID: {}", orderId);
                    return new RuntimeException("Payment record not found for Order ID: " + orderId);
                });

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

        // Use sentinel check — no enum comparison needed
        if (CREDITS_PLAN.equalsIgnoreCase(payment.getPlanName())) {
            creditService.addCredits(payment.getResellerId(), payment.getCreditAmount(), orderId);
            log.info("Credits added successfully for reseller: {}", payment.getResellerId());
        } else {
            SubscriptionActivationRequest activationRequest = new SubscriptionActivationRequest();
            activationRequest.setDeviceId(payment.getDeviceId().toString());
            activationRequest.setPlanName(payment.getPlanName());
            subscriptionService.activateSubscription(activationRequest);
            log.info("PayPal Subscription activated successfully for device: {}", payment.getDeviceId());
        }
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
    public Payments captureOrder(String orderId) {
        if (orderId == null) {
            log.error("Capture failed: Order ID is null");
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        final String finalOrderId = orderId.trim();
        log.info("Attempting to capture PayPal order: {}", finalOrderId);

        Payments payment = null;
        int maxRetries = 2;
        for (int i = 0; i < maxRetries; i++) {
            payment = paymentRepository.findByPaypalOrderId(finalOrderId).orElse(null);
            if (payment != null)
                break;

            if (i < maxRetries - 1) {
                log.warn("Payment record not found for Order ID: {} on attempt {}. Retrying in 500ms...",
                        finalOrderId, i + 1);
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
            return payment;
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
                                        fee = new BigDecimal((String) paypalFeeMap.get("value"));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to extract capture details from response for Order ID: {}", orderId, e);
                }

                processSuccessfulPaypalPayment(orderId, captureId, fee);
                return paymentRepository.findByPaypalOrderId(finalOrderId).orElse(payment);
            } else {
                log.error("PayPal capture API returned non-success status: {}. Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("PayPal capture failed with status: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 422 && body.contains("ORDER_ALREADY_CAPTURED")) {
                log.warn("PayPal order {} was already captured (422). Checking DB to complete if still PENDING.",
                        orderId);
                paymentRepository.findByPaypalOrderId(finalOrderId).ifPresent(p -> {
                    if (p.getStatus() == PaymentStatus.PENDING) {
                        p.setStatus(PaymentStatus.SUCCESS);
                        paymentRepository.save(p);
                        log.info("Marked payment for order {} as SUCCESS after 422 ORDER_ALREADY_CAPTURED.",
                                finalOrderId);
                    }
                });
                return paymentRepository.findByPaypalOrderId(finalOrderId).orElse(payment);
            }
            log.error("PayPal API Error during capture! Status: {}, Body: {}", e.getStatusCode(), body);
            throw new RuntimeException("PayPal API capture error: " + body, e);
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
        String orderId = extractOrderId(payload);

        if (orderId == null) {
            java.util.Map<String, Object> resource = (java.util.Map<String, Object>) payload.get("resource");
            java.util.List<java.util.Map<String, Object>> disputedTransactions = (java.util.List<java.util.Map<String, Object>>) resource
                    .get("disputed_transactions");

            if (disputedTransactions != null && !disputedTransactions.isEmpty()) {
                String captureId = (String) disputedTransactions.get(0).get("buyer_transaction_id");
                if (captureId == null)
                    captureId = (String) disputedTransactions.get(0).get("seller_transaction_id");

                if (captureId != null) {
                    log.info("Found Capture ID {} in dispute. Searching payment...", captureId);
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
        return null;
    }

    private void processCaptureCompleted(java.util.Map<String, Object> payload) {
        java.util.Map<String, Object> resource = (java.util.Map<String, Object>) payload.get("resource");
        String captureId = (String) resource.get("id");
        BigDecimal fee = extractFee(resource);

        // Strategy 1: supplementary_data.related_ids.order_id (present on some
        // accounts/regions)
        String orderId = extractOrderId(payload);

        // Strategy 2: parse order_id from resource.links where rel == "up"
        if (orderId == null) {
            orderId = extractOrderIdFromLinks(resource);
        }

        if (orderId != null) {
            log.info("Extracted Order ID {}, Capture ID {}, Fee {} from capture webhook", orderId, captureId, fee);
            processSuccessfulPaypalPayment(orderId, captureId, fee);
            return;
        }

        // Strategy 3: look up by the capture ID that was stored during the redirect
        // capture
        log.warn("Could not extract Order ID from PAYMENT.CAPTURE.COMPLETED webhook. "
                + "Falling back to capture-ID lookup for captureId={}", captureId);
        if (captureId != null) {
            paymentRepository.findByPaypalCaptureId(captureId).ifPresentOrElse(
                    payment -> {
                        if (payment.getStatus() != PaymentStatus.SUCCESS) {
                            processSuccessfulPaypalPayment(payment.getPaypalOrderId(), captureId, fee);
                        } else {
                            log.info("Payment for captureId {} is already SUCCESS — skipping.", captureId);
                        }
                    },
                    () -> log.error("CRITICAL: No payment record found for captureId={}. Manual intervention required.",
                            captureId));
        } else {
            log.error("CRITICAL: Could not determine Order ID or Capture ID from PAYMENT.CAPTURE.COMPLETED payload: {}",
                    payload);
        }
    }

    /**
     * Extracts the PayPal order ID from the resource links array.
     * PayPal includes a link with rel=\"up\" whose href ends with the order ID,
     * e.g. https://api-m.paypal.com/v2/checkout/orders/{orderId}
     */
    private String extractOrderIdFromLinks(java.util.Map<String, Object> resource) {
        java.util.List<java.util.Map<String, Object>> links = (java.util.List<java.util.Map<String, Object>>) resource
                .get("links");
        if (links == null)
            return null;
        for (java.util.Map<String, Object> link : links) {
            if ("up".equals(link.get("rel"))) {
                String href = (String) link.get("href");
                if (href != null && href.contains("/checkout/orders/")) {
                    String id = href.substring(href.lastIndexOf('/') + 1);
                    log.info("Extracted Order ID {} from capture webhook 'up' link.", id);
                    return id;
                }
            }
        }
        return null;
    }

    /** Extracts the PayPal fee from a capture resource map. */
    private BigDecimal extractFee(java.util.Map<String, Object> resource) {
        java.util.Map<String, Object> breakdown = (java.util.Map<String, Object>) resource
                .get("seller_receivable_breakdown");
        if (breakdown != null) {
            java.util.Map<String, Object> feeMap = (java.util.Map<String, Object>) breakdown.get("paypal_fee");
            if (feeMap != null) {
                return new BigDecimal((String) feeMap.get("value"));
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean verifyWebhookSignature(java.util.Map<String, Object> payload,
            java.util.Map<String, String> headers) {
        try {
            String accessToken = getAccessToken();
            org.springframework.http.HttpHeaders authHeaders = new org.springframework.http.HttpHeaders();
            authHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            authHeaders.setBearerAuth(accessToken);

            java.util.Map<String, String> lowerCaseHeaders = new java.util.HashMap<>();
            headers.forEach((k, v) -> lowerCaseHeaders.put(k.toLowerCase(), v));

            java.util.Map<String, Object> verificationRequest = new java.util.HashMap<>();
            verificationRequest.put("auth_algo", lowerCaseHeaders.get("paypal-auth-algo"));
            verificationRequest.put("cert_url", lowerCaseHeaders.get("paypal-cert-url"));
            verificationRequest.put("transmission_id", lowerCaseHeaders.get("paypal-transmission-id"));
            verificationRequest.put("transmission_sig", lowerCaseHeaders.get("paypal-transmission-sig"));
            verificationRequest.put("transmission_time", lowerCaseHeaders.get("paypal-transmission-time"));
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
    public java.util.List<InvoiceResponse> getAllInvoicesByDevice(String deviceId) {
        log.info("Fetching all invoices for device: {}", deviceId);
        UUID resolvedDeviceId = deviceService.resolveDeviceId(deviceId);

        java.util.List<Payments> payments = paymentRepository.findAllByDeviceIdOrderByCreatedAtDesc(resolvedDeviceId);

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

    @Override
    public java.util.List<com.iptv.wiseplayer.dto.response.PlanResponse> getActivePlans() {
        log.info("Fetching all active plans for public access");
        return planConfigRepository.findAllByActiveTrue().stream()
                .map(this::convertToPlanResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.iptv.wiseplayer.dto.response.PlanResponse convertToPlanResponse(SubscriptionPlanConfig plan) {
        com.iptv.wiseplayer.dto.response.PlanResponse response = new com.iptv.wiseplayer.dto.response.PlanResponse();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setDurationDays(plan.getDurationDays());
        response.setPrice(plan.getPrice());
        response.setCurrency(plan.getCurrency());
        response.setDescription(plan.getDescription());
        response.setActive(plan.isActive());
        response.setCreatedAt(plan.getCreatedAt());
        return response;
    }

    @Override
    public String generateInvoiceNumber(UUID paymentId) {
        return "INV-" + paymentId.toString().substring(0, 8).toUpperCase();
    }

    private InvoiceResponse mapToInvoiceResponse(Payments payment) {
        InvoiceResponse response = new InvoiceResponse();
        response.setInvoiceNumber(generateInvoiceNumber(payment.getId()));
        response.setPaymentId(payment.getId());
        response.setDeviceId(payment.getDeviceId());
        response.setTransactionDate(payment.getCreatedAt());
        response.setStatus(payment.getStatus());
        response.setPlanName(payment.getPlanName());
        response.setPlanDisplayName(getPlanDisplayName(payment.getPlanName()));
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency() != null ? payment.getCurrency() : "EUR");
        response.setPaymentMethod("PayPal");
        response.setPaypalOrderId(payment.getPaypalOrderId());
        response.setPaypalCaptureId(payment.getPaypalCaptureId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }

    /**
     * Returns a human-readable display name for the plan.
     * Tries the plan config description first; falls back to capitalizing the name.
     */
    private String getPlanDisplayName(String planName) {
        if (planName == null)
            return "Unknown Plan";
        if (CREDITS_PLAN.equalsIgnoreCase(planName))
            return "Credit Top-Up";
        return planConfigRepository.findByName(planName)
                .map(cfg -> cfg.getDescription() != null ? cfg.getDescription() : cfg.getName())
                .orElse(planName);
    }

    @Override
    public byte[] generateInvoicePdf(String invoiceNumber, String deviceId) {
        log.info("Generating PDF for invoice: {} for device: {}", invoiceNumber, deviceId);

        // Extract ID prefix from invoice Number INV-XXXXXXX
        String idPrefix = invoiceNumber.startsWith("INV-") ? invoiceNumber.substring(4) : invoiceNumber;

        Payments targetPayment = null;  

        // If deviceId is provided and valid, try to find by device first (security check)
        if (deviceId != null && !deviceId.trim().isEmpty() && !"null".equalsIgnoreCase(deviceId)) {
            UUID resolvedDeviceId = deviceService.resolveDeviceId(deviceId);
            java.util.List<Payments> allPayments = paymentRepository.findAllByDeviceIdOrderByCreatedAtDesc(resolvedDeviceId);
            for (Payments p : allPayments) {
                if (p.getId().toString().toUpperCase().startsWith(idPrefix.toUpperCase())) {
                    targetPayment = p;
                    break;
                }
            }
        }

        // Fallback: search by prefix directly (for resellers or if device check was skipped)
        if (targetPayment == null) {
            java.util.List<Payments> paymentsByPrefix = paymentRepository.findByIdPrefix(idPrefix.toLowerCase());
            if (!paymentsByPrefix.isEmpty()) {
                targetPayment = paymentsByPrefix.get(0);
            }
        }

        if (targetPayment == null) {
            throw new ResourceNotFoundException("Invoice not found or access denied.");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // Title
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Company & Invoice Info
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20f);

            PdfPCell companyCell = new PdfPCell(new Phrase("WisePlayer IPTV", titleFont));
            companyCell.setBorder(PdfPCell.NO_BORDER);
            infoTable.addCell(companyCell);

            PdfPCell invoiceDetailsCell = new PdfPCell();
            invoiceDetailsCell.setBorder(PdfPCell.NO_BORDER);
            invoiceDetailsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceDetailsCell.addElement(new Paragraph("Invoice Number: " + invoiceNumber, normalFont));
            invoiceDetailsCell.addElement(new Paragraph("Date: " + targetPayment.getCreatedAt().toLocalDate(), normalFont));
            invoiceDetailsCell.addElement(new Paragraph("Status: " + targetPayment.getStatus(), normalFont));
            infoTable.addCell(invoiceDetailsCell);
            document.add(infoTable);

            // Customer Info
            document.add(new Paragraph("Billed To:", headerFont));
            if (targetPayment.getDeviceId() != null) {
                document.add(new Paragraph("Device ID: " + targetPayment.getDeviceId(), normalFont));
            } else if (targetPayment.getResellerId() != null) {
                document.add(new Paragraph("Reseller ID: " + targetPayment.getResellerId(), normalFont));
            } else {
                document.add(new Paragraph("Customer: Generic", normalFont));
            }
            document.add(new Paragraph("Payment Method: PayPal", normalFont));
            if (targetPayment.getPaypalOrderId() != null) {
                document.add(new Paragraph("Transaction ID: " + targetPayment.getPaypalOrderId(), normalFont));
            }
            document.add(new Paragraph(" ")); // blank line

            // Item Table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{60, 20, 20});
            table.setSpacingBefore(20f);

            PdfPCell c1 = new PdfPCell(new Phrase("Description", headerFont));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setPadding(8f);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase("Currency", headerFont));
            c2.setHorizontalAlignment(Element.ALIGN_CENTER);
            c2.setPadding(8f);
            table.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase("Amount", headerFont));
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            c3.setPadding(8f);
            table.addCell(c3);

            table.setHeaderRows(1);

            // Item Row
            String planDesc = getPlanDisplayName(targetPayment.getPlanName());
            PdfPCell cell1 = new PdfPCell(new Phrase("Subscription Plan - " + planDesc, normalFont));
            cell1.setPadding(8f);
            cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell1);

            PdfPCell cell2 = new PdfPCell(new Phrase("EUR", normalFont));
            cell2.setPadding(8f);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell2);

            PdfPCell cell3 = new PdfPCell(new Phrase(targetPayment.getAmount().toString(), normalFont));
            cell3.setPadding(8f);
            cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell3);

            document.add(table);

            // Totals
            Paragraph totals = new Paragraph("Total: " + targetPayment.getAmount().toString() + " EUR", titleFont);
            totals.setAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingBefore(20f);
            document.add(totals);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Could not generate invoice PDF", e);
        }
    }
}
