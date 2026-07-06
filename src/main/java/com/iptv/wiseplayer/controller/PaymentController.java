package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.CheckoutRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.security.DeviceAuthenticationToken;
import com.iptv.wiseplayer.domain.entity.Payments;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * PaymentController handles all payment-related endpoints including PayPal
 * integration,
 * checkout sessions, and invoice retrieval.
 */
@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "Endpoints for subscription payments and checkout sessions")
public class PaymentController {

    @Value("${paypal.frontend-url:https://wise-player.com}")
    private String frontendUrl;

//    @Value("${APP_BASE_URL}")
    @Value("${APP_BASE_URL:https://admin.wise-player.com}")
    private String appBaseUrl;

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create Checkout Session", description = "Initiates a PayPal checkout session for a device subscription.")
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutSession(@Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = paymentService.createCheckoutSession(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Public Checkout Session", description = "Initiates a PayPal checkout session for a subscription (Public access for web users).")
    @PostMapping("/public/checkout")
    public ResponseEntity<CheckoutResponse> createPublicCheckoutSession(@Valid @RequestBody CheckoutRequest request) {
        String publicReturnUrl = appBaseUrl + "/api/payment/paypal/public/success";
        CheckoutResponse response = paymentService.createCheckoutSession(request, publicReturnUrl);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get All Active Plans", description = "Retrieves a list of all active subscription plans for public access.")
    @GetMapping("/public/plans")
    public ResponseEntity<java.util.List<com.iptv.wiseplayer.dto.response.PlanResponse>> getActivePlans() {
        return ResponseEntity.ok(paymentService.getActivePlans());
    }

    @Operation(summary = "Stripe Webhook (Disabled)", description = "Legacy endpoint for Stripe events (currently disabled).", hidden = true)
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        return ResponseEntity.ok("Disabled");
    }

    @Operation(summary = "PayPal Webhook", description = "Endpoint to handle asynchronous payment events from PayPal (Captures, Refunds, Disputes).")
    @PostMapping("/paypal/webhook")
    public ResponseEntity<String> handlePaypalWebhook(@RequestBody java.util.Map<String, Object> payload,
            @RequestHeader org.springframework.http.HttpHeaders headers) {
        paymentService.handlePaypalWebhook(payload, headers.toSingleValueMap());
        return ResponseEntity.ok("OK");
    }

    @Operation(summary = "PayPal Success Redirect", description = "Internal callback after PayPal approval. Handles dynamic redirection for resellers vs app users.")
    @GetMapping("/paypal/success")
    public ResponseEntity<Object> paypalSuccess(@RequestParam("token") String orderId,
            @RequestParam("PayerID") String payerId) {
        Payments payment = paymentService.captureOrder(orderId.trim());

        // If it's a reseller/sub-reseller (CREDITS plan), redirect to the reseller portal
        if (payment != null && "CREDITS".equalsIgnoreCase(payment.getPlanName())) {
            String invoiceNo = paymentService.generateInvoiceNumber(payment.getId());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(String.format("%s/purchase-credit?paymentStatus=success&invoiceNo=%s", frontendUrl, invoiceNo)))
                    .build();
        }

        // For APK/App Users: Stay on API URL as requested (previous flow)
        return ResponseEntity.ok("Payment processed successfully. You can return to the app.");
    }

    @Operation(summary = "PayPal Public Success Redirect", description = "Internal callback for public web checkouts. Redirects users back to the frontend with detailed info for the success dialog.")
    @GetMapping("/paypal/public/success")
    public ResponseEntity<Object> paypalPublicSuccess(@RequestParam("token") String orderId,
            @RequestParam("PayerID") String payerId) {
        Payments payment = paymentService.captureOrder(orderId.trim());

        // Construct invoice number using service logic
        String invoiceNo = paymentService.generateInvoiceNumber(payment.getId());

        // Append details as query params for the frontend to show the "Success" dialog
        String redirectUrl = String.format("%s/home?paymentStatus=success&invoiceNo=%s&deviceId=%s",
                frontendUrl,
                invoiceNo,
                payment.getDeviceId());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @Operation(summary = "PayPal Cancel Redirect", description = "Internal callback if user cancels. Handles dynamic redirection.")
    @GetMapping("/paypal/cancel")
    public ResponseEntity<Object> paypalCancel(@RequestParam(value = "token", required = false) String token) {
        if (token != null) {
            // For resellers, redirect back to portal
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/purchase-credit?paymentStatus=cancelled"))
                    .build();
        }
        return ResponseEntity.ok("Payment cancelled.");
    }

    @Operation(summary = "Get All Invoices", description = "Retrieves all detailed invoices for a specific device. Requires matching device token or Admin role.")
    @GetMapping("/invoices")
    public ResponseEntity<java.util.List<com.iptv.wiseplayer.dto.response.InvoiceResponse>> getAllInvoices(
            @RequestParam String deviceId) {
        validateAccess(deviceId);
        return ResponseEntity.ok(paymentService.getAllInvoicesByDevice(deviceId));
    }

    @Operation(summary = "Get Current Active Invoice", description = "Retrieves the latest successful invoice for a specific device. Requires matching device token or Admin role.")
    @GetMapping("/invoice/current")
    public ResponseEntity<com.iptv.wiseplayer.dto.response.InvoiceResponse> getCurrentInvoice(
            @RequestParam String deviceId) {
        validateAccess(deviceId);
        com.iptv.wiseplayer.dto.response.InvoiceResponse invoice = paymentService.getCurrentInvoice(deviceId);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/invoice/{invoiceNumber}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable String invoiceNumber,
            @RequestParam(required = false) String deviceId) {
        if (deviceId != null && !deviceId.trim().isEmpty() && !"null".equalsIgnoreCase(deviceId)) {
            validateAccess(deviceId);
        }
        byte[] pdfBytes = paymentService.generateInvoicePdf(invoiceNumber, deviceId);
        
        return createPdfResponse(pdfBytes, invoiceNumber);
    }

    @GetMapping("/public/invoice/{invoiceNumber}/pdf")
    public ResponseEntity<byte[]> publicDownloadInvoicePdf(
            @PathVariable String invoiceNumber,
            @RequestParam(required = false) String deviceId) {
        // No validateAccess(deviceId) here — the service already verifies the invoice belongs to the device.
        byte[] pdfBytes = paymentService.generateInvoicePdf(invoiceNumber, deviceId);
        
        return createPdfResponse(pdfBytes, invoiceNumber);
    }

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfBytes, String invoiceNumber) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + invoiceNumber + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /**
     * Verifies that the authenticated principal has access to the requested
     * deviceId.
     * Prevents IDOR (Insecure Direct Object Reference) attacks.
     */
    private void validateAccess(String requestedDeviceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return;

        // Allow Admin/Super-Admin to access any device
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isAdmin)
            return;

        // For Device users, verify requested deviceId matches authenticated device
        if (auth instanceof DeviceAuthenticationToken) {
            String authedDeviceId = ((DeviceAuthenticationToken) auth).getDevice().getDeviceId().toString();
            if (!authedDeviceId.equalsIgnoreCase(requestedDeviceId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access denied to device: " + requestedDeviceId);
            }
        }
    }
}
