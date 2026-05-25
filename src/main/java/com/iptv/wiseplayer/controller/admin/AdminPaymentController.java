package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.dto.response.AdminPaymentResponse;
import com.iptv.wiseplayer.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "Admin Payment Management", description = "Endpoints for monitoring payments and revenue")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @Operation(summary = "List All Payments", description = "Retrieves a paginated list of all payment attempts.")
    @GetMapping
    public ResponseEntity<Page<AdminPaymentResponse>> getAllPayments(
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(adminPaymentService.getAllPayments(paymentId, deviceId, status, pageable));
    }

    @Operation(summary = "Get Payment Stats", description = "Retrieves high-level payment and revenue statistics.")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPaymentStats() {
        return ResponseEntity.ok(adminPaymentService.getPaymentStats());
    }
}
