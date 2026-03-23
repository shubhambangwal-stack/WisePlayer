package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Endpoints for enhanced system reports and analytics")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "Revenue Report", description = "Retrieves revenue statistics for a given date range.")
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(adminReportService.getRevenueReport(from, to));
    }

    @Operation(summary = "Device Report", description = "Retrieves device registration statistics for a given date range.")
    @GetMapping("/devices")
    public ResponseEntity<Map<String, Object>> getDeviceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(adminReportService.getDeviceReport(from, to));
    }

    @Operation(summary = "Subscription Report", description = "Retrieves subscription statistics for a given date range.")
    @GetMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> getSubscriptionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(adminReportService.getSubscriptionReport(from, to));
    }

    @Operation(summary = "Reseller Report", description = "Retrieves a summary report for all resellers.")
    @GetMapping("/resellers")
    public ResponseEntity<List<Map<String, Object>>> getResellerReport() {
        return ResponseEntity.ok(adminReportService.getResellerReport());
    }
}
