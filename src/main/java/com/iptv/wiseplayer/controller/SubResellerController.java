package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.ResellerActivationRequestDto;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.service.ResellerService;
import com.iptv.wiseplayer.domain.entity.Admin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sub-reseller")
@PreAuthorize("hasAnyAuthority('ROLE_SUB_RESELLER', 'ROLE_RESELLER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@Tag(name = "Sub-Reseller API", description = "Endpoints for Sub-Reseller Management")
public class SubResellerController {

    private final ResellerService resellerService;
    private final AdminRepository adminRepository;
    private final com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository;

    public SubResellerController(ResellerService resellerService,
            AdminRepository adminRepository,
            com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository) {
        this.resellerService = resellerService;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Sub-Reseller Dashboard", description = "Get overview metrics for the sub-reseller")
    public ResponseEntity<ResellerDashboardResponse> getDashboard() {
        return ResponseEntity.ok(resellerService.getDashboardOverview(getCurrentSubResellerId()));
    }

    @PostMapping("/user")
    @Operation(summary = "Create End User", description = "Register an end user device under this sub-reseller")
    public ResponseEntity<DeviceRegistrationResponse> createEndUser(
            @Valid @RequestBody DeviceRegistrationRequest request) {
        return ResponseEntity.ok(resellerService.createEndUser(getCurrentSubResellerId(), request));
    }

    @GetMapping("/users")
    @Operation(summary = "Get Users", description = "Get a list of all devices/users managed by this sub-reseller")
    public ResponseEntity<List<Device>> getUsers() {
        return ResponseEntity.ok(resellerService.getResellerUsers(getCurrentSubResellerId()));
    }

    @PostMapping("/activation-request")
    @Operation(summary = "Submit Activation Request", description = "Submit a request to activate a user/device subscription")
    public ResponseEntity<ActivationRequest> submitRequest(@Valid @RequestBody ResellerActivationRequestDto request) {
        return ResponseEntity.ok(resellerService.submitActivationRequest(getCurrentSubResellerId(), request));
    }

    @GetMapping("/activation-request")
    @Operation(summary = "Get Activation Requests", description = "Get a list of all activation requests submitted by this sub-reseller")
    public ResponseEntity<List<ActivationRequest>> getRequests() {
        return ResponseEntity.ok(resellerService.getResellerRequests(getCurrentSubResellerId()));
    }

    private UUID getCurrentSubResellerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(username)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(username)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Reseller not found for: " + username));
    }
}
