package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.ResellerActivationRequestDto;
import com.iptv.wiseplayer.dto.request.ResellerLoginRequest;
import com.iptv.wiseplayer.dto.request.ResellerRegisterRequest;
import com.iptv.wiseplayer.dto.request.SubResellerCreateRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.service.ResellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.iptv.wiseplayer.dto.request.VerifyOtpRequest;
import com.iptv.wiseplayer.dto.request.ResellerForgotPasswordRequest;
import com.iptv.wiseplayer.dto.request.ResellerResetPasswordRequest;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reseller")
@Tag(name = "Reseller API", description = "Endpoints for Reseller Management")
public class ResellerController {

    private static final Logger log = LoggerFactory.getLogger(ResellerController.class);

    private final ResellerService resellerService;
    private final AdminRepository adminRepository;
    private final com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository;

    public ResellerController(ResellerService resellerService,
            AdminRepository adminRepository,
            com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository) {
        this.resellerService = resellerService;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    // --- Authentication Endpoints ---

    @PostMapping("/login")
    @Operation(summary = "Reseller Login", description = "Authenticate a reseller and get a JWT token")
    public ResponseEntity<AdminAuthResponse> login(@Valid @RequestBody ResellerLoginRequest request) {
        return ResponseEntity.ok(resellerService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Reseller Registration", description = "Register a new reseller account")
    public ResponseEntity<AdminAuthResponse> register(@Valid @RequestBody ResellerRegisterRequest request) {
        return ResponseEntity.ok(resellerService.register(request));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify Email OTP", description = "Send JWT from register + OTP from email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(resellerService.verifyEmail(request, username));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Resend verification OTP. Requires JWT from login response.")
    public ResponseEntity<Map<String, String>> resendOtp() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(resellerService.resendOtp(username));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Send password reset link to reseller email")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ResellerForgotPasswordRequest request) {
        resellerService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("success", "true", "message", "Reset link sent to your email"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Reset reseller password using token from email")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResellerResetPasswordRequest request) {
        resellerService.resetPassword(request);
        return ResponseEntity.ok(Map.of("success", "true", "message", "Password reset successfully"));
    }

    // --- Reseller Management Endpoints ---

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PostMapping("/user")
    @Operation(summary = "Create End User", description = "Register an end user device under this reseller")
    public ResponseEntity<java.util.Map<String, Object>> createEndUser(
            @Valid @RequestBody DeviceRegistrationRequest request) {
        return ResponseEntity.ok(resellerService.createEndUser(getCurrentResellerId(), request));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @GetMapping("/dashboard")
    @Operation(summary = "Reseller Dashboard", description = "Get overview metrics for the reseller")
    public ResponseEntity<ResellerDashboardResponse> getDashboard() {
        return ResponseEntity.ok(resellerService.getDashboardOverview(getCurrentResellerId()));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @GetMapping("/users")
    @Operation(summary = "Get Users", description = "Get devices managed by this reseller with filters for status, plan, registered and expiry date range")
    public ResponseEntity<org.springframework.data.domain.Page<Device>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.iptv.wiseplayer.domain.enums.DeviceStatus status,
            @RequestParam(required = false) String subscription,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate registeredFrom,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate registeredTo,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate expiresFrom,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate expiresTo,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getResellerUsers(
                getCurrentResellerId(), search, status, subscription,
                registeredFrom, registeredTo, expiresFrom, expiresTo, pageable));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PutMapping("/users/{deviceId}/disable")
    @Operation(summary = "Toggle User Status", description = "Toggle a specific device/user between active and inactive")
    public ResponseEntity<Void> disableUser(@PathVariable UUID deviceId) {
        resellerService.disableUser(getCurrentResellerId(), deviceId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PostMapping("/sub-resellers")
    @Operation(summary = "Create Sub-Reseller", description = "Create a new sub-reseller under this reseller")
    public ResponseEntity<Admin> createSubReseller(@Valid @RequestBody SubResellerCreateRequest request) {
        return ResponseEntity.ok(resellerService.createSubReseller(getCurrentResellerId(), request));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @GetMapping("/sub-resellers")
    public ResponseEntity<org.springframework.data.domain.Page<Admin>> getSubResellers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) java.math.BigDecimal minCredits,
            @RequestParam(required = false) java.math.BigDecimal maxCredits,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getSubResellers(
                getCurrentResellerId(), search, status, fromDate, toDate, minCredits, maxCredits, pageable));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PutMapping("/sub-resellers/{id}")
    @Operation(summary = "Update Sub-Reseller", description = "Update details for a specific sub-reseller")
    public ResponseEntity<Void> updateSubReseller(@PathVariable UUID id,
            @Valid @RequestBody com.iptv.wiseplayer.dto.request.SubResellerUpdateRequest request) {
        resellerService.updateSubReseller(getCurrentResellerId(), id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PatchMapping("/sub-resellers/{id}/status")
    @Operation(summary = "Toggle Sub-Reseller Status", description = "Activate or deactivate a sub-reseller account")
    public ResponseEntity<Void> toggleSubResellerStatus(@PathVariable UUID id) {
        resellerService.toggleSubResellerStatus(getCurrentResellerId(), id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @DeleteMapping("/sub-resellers/{id}")
    @Operation(summary = "Delete Sub-Reseller", description = "Permanently delete a sub-reseller under this reseller")
    public ResponseEntity<Map<String, Object>> deleteSubReseller(@PathVariable UUID id) {
        resellerService.deleteSubReseller(getCurrentResellerId(), id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Sub-reseller deleted successfully"));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PostMapping("/activation-request")
    @Operation(summary = "Submit Activation Request", description = "Submit a request to activate a user/device subscription")
    public ResponseEntity<ActivationRequest> submitRequest(@Valid @RequestBody ResellerActivationRequestDto request) {
        return ResponseEntity.ok(resellerService.submitActivationRequest(getCurrentResellerId(), request));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @GetMapping("/activation-request")
    @Operation(summary = "Get Activation Requests", description = "Get a list of all activation requests submitted by this reseller")
    public ResponseEntity<org.springframework.data.domain.Page<com.iptv.wiseplayer.dto.response.ActivationRequestResponse>> getRequests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) java.math.BigDecimal minCredits,
            @RequestParam(required = false) java.math.BigDecimal maxCredits,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getResellerRequests(
                getCurrentResellerId(), search, status, planName, fromDate, toDate, minCredits, maxCredits, pageable));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @DeleteMapping("/activation-request/{id}")
    @Operation(summary = "Delete Activation Request", description = "Delete a pending or rejected activation request")
    public ResponseEntity<Map<String, Object>> deleteActivationRequest(@PathVariable UUID id) {
        resellerService.deleteActivationRequest(getCurrentResellerId(), id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Activation request deleted successfully"));
    }

    private UUID getCurrentResellerId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(identifier)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(identifier)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found for: " + identifier));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @DeleteMapping("/users/{deviceId}/detach")
    @Operation(summary = "Detach Device", description = "Remove device from reseller. Subscription stays intact.")
    public ResponseEntity<Map<String, Object>> detachDevice(@PathVariable UUID deviceId) {
        resellerService.detachDevice(getCurrentResellerId(), deviceId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Device detached successfully"));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PatchMapping("/users/{deviceId}/lock")
    @Operation(summary = "Lock/Unlock Device", description = "Lock device to block all access. Call again to unlock.")
    public ResponseEntity<Map<String, Object>> lockDevice(@PathVariable UUID deviceId) {
        resellerService.disableUser(getCurrentResellerId(), deviceId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Device lock status toggled"));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PatchMapping("/users/{deviceId}/cancel-subscription")
    @Operation(summary = "Cancel Subscription", description = "Permanently cancel subscription. Cannot be renewed.")
    public ResponseEntity<Map<String, Object>> cancelSubscription(@PathVariable UUID deviceId) {
        resellerService.cancelSubscription(getCurrentResellerId(), deviceId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Subscription cancelled successfully"));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PatchMapping("/users/{deviceId}/pause-subscription")
    @Operation(summary = "Pause/Resume Subscription", description = "Toggle subscription between PAUSED and ACTIVE.")
    public ResponseEntity<Map<String, Object>> pauseResumeSubscription(@PathVariable UUID deviceId) {
        resellerService.pauseResumeSubscription(getCurrentResellerId(), deviceId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Subscription pause/resume toggled"));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PutMapping("/sub-resellers/bulk-permissions")
    @Operation(summary = "Bulk Update Sub-Reseller Permissions", description = "Updates permissions for all sub-resellers under this reseller at once.")
    public ResponseEntity<?> updateSubResellersBulkPermissions(
            @RequestBody com.iptv.wiseplayer.dto.request.UpdateResellerRequest request) {
        resellerService.updateSubResellersBulkPermissions(getCurrentResellerId(), request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Permissions updated successfully for all sub-resellers"));
    }

    // -------------------------------------------------------------------------
    // Consistent crud-permissions endpoints
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Bulk: update CRUD for ALL sub-resellers under this reseller",
        description = "Updates canCreate/canRead/canUpdate/canDelete for every sub-reseller belonging to this reseller. " +
                      "Escalation rule: caller cannot grant flags they don't own. Null fields are skipped."
    )
    @PutMapping("/crud-permissions/bulk")
    public ResponseEntity<?> bulkUpdateSubResellerPermissions(
            @RequestBody com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest request) {
        // Convert to UpdateResellerRequest for the existing service method
        com.iptv.wiseplayer.dto.request.UpdateResellerRequest bulk =
                new com.iptv.wiseplayer.dto.request.UpdateResellerRequest();
        bulk.setCanCreate(request.getCanCreate());
        bulk.setCanRead(request.getCanRead());
        bulk.setCanUpdate(request.getCanUpdate());
        bulk.setCanDelete(request.getCanDelete());
        resellerService.updateSubResellersBulkPermissions(getCurrentResellerId(), bulk);
        return ResponseEntity.ok(Map.of("success", true,
                "message", "CRUD permissions updated for all sub-resellers."));
    }

    @Operation(
        summary = "Individual: update CRUD for a specific sub-reseller by ID",
        description = "Changes only the CRUD flags for a specific sub-reseller identified by UUID. " +
                      "Ownership rule: the sub-reseller must belong to the calling reseller. " +
                      "Escalation rule: caller cannot grant flags they don't own. Null fields are skipped."
    )
    @PatchMapping("/crud-permissions/{id}")
    public ResponseEntity<?> updateSubResellerPermissionsById(
            @PathVariable UUID id,
            @RequestBody com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest request) {
        resellerService.updateSubResellerPermissionsById(getCurrentResellerId(), id, request);
        return ResponseEntity.ok(Map.of("success", true,
                "message", "CRUD permissions updated for sub-reseller " + id));
    }
}
