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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

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

        UUID deviceId = deviceService.getDeviceIdByFingerprint(request.getDeviceId());

        // For simplicity, we define static prices here. In production, these should be
        // from DB or Stripe Product ID.
        long amountInCents = 0;
        switch (request.getPlan()) {
            case MONTHLY -> amountInCents = 500; // $5.00
            case QUARTERLY -> amountInCents = 1200; // $12.00
            case YEARLY -> amountInCents = 4000; // $40.00
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://wiseplayer.api/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://wiseplayer.api/payment/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amountInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(request.getPlan().name() + " Subscription")
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
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid webhook signature");
        }

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
        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new RuntimeException("Payment record not found for session: " + session.getId()));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return; // Already processed
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setStripeEventId(eventId);
        paymentRepository.save(payment);

        // Activate Subscription
        String fingerprint = session.getMetadata().get("deviceId");
        String planStr = session.getMetadata().get("plan");

        SubscriptionActivationRequest activationRequest = new SubscriptionActivationRequest();
        activationRequest.setDeviceId(fingerprint);
        activationRequest.setPlan(com.iptv.wiseplayer.domain.enums.SubscriptionPlan.valueOf(planStr));

        subscriptionService.activateSubscription(activationRequest);
    }
}
