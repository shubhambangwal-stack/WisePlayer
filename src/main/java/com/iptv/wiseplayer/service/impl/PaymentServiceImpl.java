package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Payment;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.dto.request.CheckoutRequest;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.repository.PaymentRepository;
import com.iptv.wiseplayer.service.DeviceService;
import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.service.SubscriptionService;
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
        // Stripe implementation commented out
        /*
         * Stripe.apiKey = stripeApiKey;
         * ...
         */

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
        purchaseUnit.put("reference_id", deviceId.toString());

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

            Payment payment = new Payment();
            payment.setDeviceId(deviceId);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaypalOrderId(orderId);
            payment.setAmount(amount);
            payment.setPlan(request.getPlan());
            paymentRepository.save(payment);

            return new CheckoutResponse(approveUrl, orderId);
        }

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

        if ("CHECKOUT.ORDER.APPROVED".equals(eventType) || "PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
            java.util.Map<String, Object> resource = (java.util.Map<String, Object>) payload.get("resource");
            String orderId = (String) resource.get("id");
            if (orderId == null && resource.containsKey("supplementary_data")) {
                // For captures, orderId might be deeper
            }

            // In a real scenario, we should call PayPal to verify the order status
            processSuccessfulPaypalPayment(orderId);
        }
    }

    private void processSuccessfulPaypalPayment(String orderId) {
        Payment payment = paymentRepository.findByPaypalOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for Order ID: " + orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS)
            return;

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        SubscriptionActivationRequest activationRequest = new SubscriptionActivationRequest();
        activationRequest.setDeviceId(payment.getDeviceId().toString());
        activationRequest.setPlan(payment.getPlan());
        subscriptionService.activateSubscription(activationRequest);

        log.info("PayPal Subscription activated for device: {}", payment.getDeviceId());
    }

    @Override
    @Transactional
    public void captureOrder(String orderId) {
        String accessToken = getAccessToken();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("{}", headers);

        try {
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    getPaypalBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture", entity, java.util.Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("PayPal Order captured successfully: {}", orderId);
                processSuccessfulPaypalPayment(orderId);
            } else {
                log.error("Failed to capture PayPal order: {}. Status: {}", orderId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error capturing PayPal order: {}", orderId, e);
            throw new RuntimeException("Error capturing PayPal order", e);
        }
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
}
