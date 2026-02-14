package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.CheckoutRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "Endpoints for subscription payments and checkout sessions")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create Checkout Session", description = "Initiates a Stripe checkout session for a subscription.")
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutSession(@RequestBody CheckoutRequest request) {
        CheckoutResponse response = paymentService.createCheckoutSession(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Stripe Webhook (Disabled)", description = "Endpoint to handle Stripe events (currently disabled).", hidden = true)
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        // Stripe disabled
        return ResponseEntity.ok("Disabled");
    }

    @Operation(summary = "PayPal Webhook", description = "Endpoint to handle asynchronous payment events from PayPal.")
    @PostMapping("/paypal/webhook")
    public ResponseEntity<String> handlePaypalWebhook(@RequestBody java.util.Map<String, Object> payload,
            @RequestHeader java.util.Map<String, String> headers) {
        paymentService.handlePaypalWebhook(payload, headers);
        return ResponseEntity.ok("OK");
    }

    @Operation(summary = "PayPal Success Redirect", description = "Handles redirection from PayPal after successful approval.")
    @GetMapping("/paypal/success")
    public ResponseEntity<String> paypalSuccess(@RequestParam("token") String orderId,
            @RequestParam("PayerID") String payerId) {
        paymentService.captureOrder(orderId);
        return ResponseEntity.ok("Payment successful and captured. You can close this window and return to the app.");
    }

    @Operation(summary = "PayPal Cancel Redirect", description = "Handles redirection from PayPal if the user cancels.")
    @GetMapping("/paypal/cancel")
    public ResponseEntity<String> paypalCancel() {
        return ResponseEntity.ok("Payment cancelled. You can close this window.");
    }
}
