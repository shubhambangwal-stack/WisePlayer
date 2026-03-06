package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import com.iptv.wiseplayer.service.AdminManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/management")
@Tag(name = "Admin Management", description = "Endpoints for SUPER_ADMIN to manage admin accounts")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;
    private final AdminRepository adminRepository;
    private final com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository;

    public AdminManagementController(AdminManagementService adminManagementService,
            AdminRepository adminRepository,
            com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository) {
        this.adminManagementService = adminManagementService;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    @Operation(summary = "Invite Admin", description = "Generates an invitation link for a new admin. Only accessible by SUPER_ADMIN.")
    @PostMapping("/invite")
    public ResponseEntity<Map<String, Object>> inviteAdmin(@RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required"));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) auth.getPrincipal();

        UUID inviterId;
        boolean isSuperAdmin = false;

        // Try SuperAdmin first
        Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository
                .findByUsername(username);
        if (superAdminOpt.isPresent()) {
            inviterId = superAdminOpt.get().getId();
            isSuperAdmin = true;
        } else {
            // Try Admin
            Admin admin = adminRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Logged in admin not found"));
            inviterId = admin.getId();
            isSuperAdmin = admin.isSuperAdmin();
        }

        String token = adminManagementService.inviteAdmin(email, inviterId, isSuperAdmin, httpRequest);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Invitation generated successfully",
                "token", token,
                "inviteUrl", "/admin/setup?token=" + token));
    }

    @Operation(summary = "Verify Invite", description = "Verifies if an invitation token is valid.")
    @GetMapping("/invite/verify")
    public ResponseEntity<Map<String, Object>> verifyInvite(@RequestParam String token) {
        try {
            adminManagementService.verifyInvite(token);
            return ResponseEntity.ok(Map.of("success", true, "valid", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Complete Setup", description = "Finalizes admin account creation with password and full name.")
    @PostMapping("/setup/complete")
    public ResponseEntity<Map<String, Object>> completeSetup(@RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String token = request.get("token");
        String password = request.get("password");
        String fullName = request.get("fullName");

        if (token == null || password == null || fullName == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Token, password, and fullName are required"));
        }

        adminManagementService.completeSetup(token, password, fullName, httpRequest);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Admin account created successfully. You can now login."));
    }
}
