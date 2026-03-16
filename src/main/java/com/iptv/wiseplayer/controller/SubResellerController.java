package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.service.ResellerService;
import com.iptv.wiseplayer.domain.entity.Admin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sub-reseller")
@PreAuthorize("hasAnyAuthority('ROLE_SUB_RESELLER', 'ROLE_RESELLER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
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
    public ResponseEntity<ResellerDashboardResponse> getDashboard() {
        return ResponseEntity.ok(resellerService.getDashboardOverview(getCurrentSubResellerId()));
    }

    @PostMapping("/user")
    public ResponseEntity<DeviceRegistrationResponse> createEndUser(@RequestBody DeviceRegistrationRequest request) {
        return ResponseEntity.ok(resellerService.createEndUser(getCurrentSubResellerId(), request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Device>> getUsers() {
        return ResponseEntity.ok(resellerService.getResellerUsers(getCurrentSubResellerId()));
    }

    @PostMapping("/activation-request")
    public ResponseEntity<ActivationRequest> submitRequest(@RequestParam UUID deviceId, @RequestParam String planName,
            @RequestParam(required = false) String status) {
        return ResponseEntity
                .ok(resellerService.submitActivationRequest(getCurrentSubResellerId(), deviceId, planName, status));
    }

    @GetMapping("/activation-request")
    public ResponseEntity<List<ActivationRequest>> getRequests() {
        return ResponseEntity.ok(resellerService.getResellerRequests(getCurrentSubResellerId()));
    }

    private UUID getCurrentSubResellerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(username)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(username)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new IllegalArgumentException("Sub-Reseller not found for: " + username));
    }
}
