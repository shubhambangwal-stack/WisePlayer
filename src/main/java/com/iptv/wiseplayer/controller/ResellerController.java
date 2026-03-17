package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.service.ResellerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reseller")
@Slf4j
public class ResellerController {

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
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(resellerService.login(body.get("username"), body.get("password")));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return ResponseEntity
                .ok(resellerService.register(body.get("username"), body.get("password"), body.get("fullName")));
    }

    // --- Reseller Management Endpoints ---

    @PostMapping("/user")
    public ResponseEntity<DeviceRegistrationResponse> createEndUser(@RequestBody DeviceRegistrationRequest request) {
        return ResponseEntity.ok(resellerService.createEndUser(getCurrentResellerId(), request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ResellerDashboardResponse> getDashboard() {
        return ResponseEntity.ok(resellerService.getDashboardOverview(getCurrentResellerId()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Device>> getUsers() {
        return ResponseEntity.ok(resellerService.getResellerUsers(getCurrentResellerId()));
    }

    @PutMapping("/users/{deviceId}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable UUID deviceId) {
        resellerService.disableUser(getCurrentResellerId(), deviceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sub-resellers")
    public ResponseEntity<Admin> createSubReseller(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(resellerService.createSubReseller(
                getCurrentResellerId(), body.get("username"), body.get("password"), body.get("fullName")));
    }

    @GetMapping("/sub-resellers")
    public ResponseEntity<List<Admin>> getSubResellers() {
        return ResponseEntity.ok(resellerService.getSubResellers(getCurrentResellerId()));
    }

    @PostMapping("/activation-request")
    public ResponseEntity<ActivationRequest> submitRequest(@RequestParam UUID deviceId, @RequestParam String planName,
            @RequestParam(required = false) String status) {
        return ResponseEntity
                .ok(resellerService.submitActivationRequest(getCurrentResellerId(), deviceId, planName, status));
    }

    @GetMapping("/activation-request")
    public ResponseEntity<List<ActivationRequest>> getRequests() {
        return ResponseEntity.ok(resellerService.getResellerRequests(getCurrentResellerId()));
    }

    private UUID getCurrentResellerId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID resellerId = adminRepository.findByUsername(identifier)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(identifier)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new IllegalArgumentException("Reseller not found for: " + identifier));
        return resellerId;
    }
}
