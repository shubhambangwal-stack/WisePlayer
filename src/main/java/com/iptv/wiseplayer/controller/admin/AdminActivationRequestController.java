package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.request.ApprovalActionRequest;
import com.iptv.wiseplayer.dto.response.ActivationRequestResponse;
import com.iptv.wiseplayer.service.AdminActivationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/activation-requests")
@Tag(name = "Admin Activation Request Management", description = "Endpoints for reviewing reseller activation requests")
public class AdminActivationRequestController {

    private final AdminActivationRequestService adminActivationRequestService;

    public AdminActivationRequestController(AdminActivationRequestService adminActivationRequestService) {
        this.adminActivationRequestService = adminActivationRequestService;
    }

    @Operation(summary = "List All Activation Requests", description = "Retrieves a paginated list of activation requests, optionally filtered by status, deviceId, or planName.")
    @GetMapping
    public ResponseEntity<Page<ActivationRequestResponse>> getAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String planName,
            Pageable pageable) {
        return ResponseEntity.ok(adminActivationRequestService.getAllRequests(status, deviceId, planName, pageable));
    }

    @Operation(summary = "Get Activation Request Details", description = "Retrieves detailed information for a specific activation request.")
    @GetMapping("/{id}")
    public ResponseEntity<ActivationRequestResponse> getRequestById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminActivationRequestService.getRequestById(id));
    }

    @Operation(summary = "Approve Activation Request", description = "Approves a pending activation request and activates the subscription.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalActionRequest request) {
        String notes = (request != null) ? request.getAdminNotes() : null;
        adminActivationRequestService.approveRequest(id, notes);
        return ResponseEntity.ok(Map.of("success", true, "message", "Activation request approved and subscription activated"));
    }

    @Operation(summary = "Reject Activation Request", description = "Rejects a pending activation request.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalActionRequest request) {
        String notes = (request != null) ? request.getAdminNotes() : null;
        adminActivationRequestService.rejectRequest(id, notes);
        return ResponseEntity.ok(Map.of("success", true, "message", "Activation request rejected"));
    }
}
