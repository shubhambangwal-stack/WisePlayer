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
    @Operation(summary = "Get Users", description = "Get a list of all devices/users managed by this reseller with optional search and status filtering")
    public ResponseEntity<org.springframework.data.domain.Page<Device>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.iptv.wiseplayer.domain.enums.DeviceStatus status,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getResellerUsers(getCurrentResellerId(), search, status, pageable));
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
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getSubResellers(
                getCurrentResellerId(), search, status, pageable));
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
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getResellerRequests(getCurrentResellerId(), search, status, planName, pageable));
    }

    private UUID getCurrentResellerId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(identifier)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(identifier)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found for: " + identifier));
    }
}
