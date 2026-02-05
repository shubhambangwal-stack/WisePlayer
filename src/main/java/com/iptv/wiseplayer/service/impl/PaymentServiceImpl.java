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

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
            DeviceService deviceService,
            SubscriptionService subscriptionService) {
        this.paymentRepository = paymentRepository;
        this.deviceService = deviceService;
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public CheckoutResponse createCheckoutSession(CheckoutRequest request) {
        Stripe.apiKey = stripeApiKey;

        UUID deviceId = deviceService.resolveDeviceId(request.getDeviceId());

        // For simplicity, we define static prices here. In production, these should be
        // from DB or Stripe Product ID.
        // Pricing: Annual 6 EUR, Lifetime 10 EUR
        long amountInCents = 0;
        switch (request.getPlan()) {
            case ANNUAL -> amountInCents = 600; // 6.00 EUR
            case LIFETIME -> amountInCents = 1000; // 10.00 EUR
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://wiseplayer.api/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://wiseplayer.api/payment/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(request.getPlan().name() + " WisePlayer Subscription")
                                        .build())
                                .build())
                        .build())
                .putMetadata("deviceId", request.getDeviceId()) // Fingerprint
                .putMetadata("plan", request.getPlan().name())
                .build();

        try {
            Session session = Session.create(params);

            Payment payment = new Payment();
            payment.setDeviceId(deviceId);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setStripeSessionId(session.getId());
            payment.setAmount(BigDecimal.valueOf(amountInCents).divide(BigDecimal.valueOf(100)));
            payment.setPlan(request.getPlan());
            paymentRepository.save(payment);

            return new CheckoutResponse(session.getUrl(), session.getId());
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        log.info("Received Stripe webhook. Signature header present: {}", sigHeader != null);
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe Webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature", e);
        } catch (Exception e) {
            log.error("Unexpected error during webhook construction: {}", e.getMessage(), e);
            throw new RuntimeException("Webhook error", e);
        }

        log.info("Processing webhook event: {} [{}]", event.getType(), event.getId());

        // Idempotency check
        if (paymentRepository.findByStripeEventId(event.getId()).isPresent()) {
            return; // Already processed
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                processSuccessfulPayment(session, event.getId());
            }
        }
    }

    private void processSuccessfulPayment(Session session, String eventId) {
        log.info("Processing successful payment for session: {}", session.getId());
        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> {
                    log.error("Payment record not found for Stripe Session ID: {}", session.getId());
                    return new RuntimeException("Payment record not found for session: " + session.getId());
                });

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment session {} already marked as SUCCESS. Skipping.", session.getId());
            return; // Already processed
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setStripeEventId(eventId);
        paymentRepository.save(payment);
        log.info("Payment updated to SUCCESS for device: {}", payment.getDeviceId());

        // Activate Subscription
        String fingerprint = session.getMetadata().get("deviceId");
        String planStr = session.getMetadata().get("plan");

        log.info("Activating subscription: deviceId={}, plan={}", fingerprint, planStr);

        if (fingerprint == null || planStr == null) {
            log.error("Missing metadata in Stripe Session: deviceId={}, plan={}", fingerprint, planStr);
            throw new RuntimeException("Missing metadata in session");
        }

        try {
            SubscriptionActivationRequest activationRequest = new SubscriptionActivationRequest();
            activationRequest.setDeviceId(fingerprint);
            activationRequest.setPlan(com.iptv.wiseplayer.domain.enums.SubscriptionPlan.valueOf(planStr));

            subscriptionService.activateSubscription(activationRequest);
            log.info("Subscription activated successfully for device: {}", fingerprint);
        } catch (Exception e) {
            log.error("Failed to activate subscription for device {}: {}", fingerprint, e.getMessage(), e);
            throw e;
        }
    }
}
