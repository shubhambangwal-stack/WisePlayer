package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.repository.AdminRepository;
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

import com.iptv.wiseplayer.dto.request.AdminInviteRequest;
import com.iptv.wiseplayer.dto.request.AdminSetupRequest;
import jakarta.validation.Valid;

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
    public ResponseEntity<Map<String, Object>> inviteAdmin(@Valid @RequestBody AdminInviteRequest request,
            HttpServletRequest httpRequest) {
        String email = request.getEmail();

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
            Admin admin = adminRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Logged in admin not found"));
            inviterId = admin.getId();
            isSuperAdmin = admin.isSuperAdmin();
        }

        AdminManagementService.InviteResult result = adminManagementService.inviteAdmin(email, inviterId, isSuperAdmin,
                httpRequest);

        Map<String, Object> data = Map.of(
                "token", result.token(),
                "inviteUrl", "/admin/setup?token=" + result.token());

        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Verify Invite", description = "Verifies if an invitation token is valid.")
    @GetMapping("/invite/verify")
    public ResponseEntity<Boolean> verifyInvite(@RequestParam String token) {
        adminManagementService.verifyInvite(token);
        return ResponseEntity.ok(true);
    }

    @Operation(summary = "Complete Setup", description = "Finalizes admin account creation with password and full name.")
    @PostMapping("/setup/complete")
    public ResponseEntity<String> completeSetup(@Valid @RequestBody AdminSetupRequest request,
            HttpServletRequest httpRequest) {
        
        adminManagementService.completeSetup(request.getToken(), request.getPassword(), request.getFullName(), httpRequest);

        return ResponseEntity.ok("Admin account created successfully. You can now login.");
    }

    @Operation(summary = "Get All Admins", description = "Retrieves a list of all admins. Accessible by SUPER_ADMIN.")
    @GetMapping("/all")
    public ResponseEntity<java.util.List<com.iptv.wiseplayer.dto.response.AdminResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminManagementService.getAllAdmins());
    }

    @Operation(summary = "Get Admin by ID", description = "Retrieves details of a specific admin by UUID. Accessible by SUPER_ADMIN.")
    @GetMapping("/{id}")
    public ResponseEntity<com.iptv.wiseplayer.dto.response.AdminResponse> getAdminById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminManagementService.getAdminById(id));
    }

    @Operation(summary = "Create Admin Direct", description = "Directly creates a new admin account without email invite workflow. Accessible by SUPER_ADMIN.")
    @PostMapping("/create")
    public ResponseEntity<com.iptv.wiseplayer.dto.response.AdminResponse> createAdminDirect(@Valid @RequestBody com.iptv.wiseplayer.dto.request.CreateAdminRequest request) {
        return ResponseEntity.ok(adminManagementService.createAdminDirect(request));
    }

    @Operation(summary = "Update Admin", description = "Updates details, role, permissions, status or credits of an existing admin by UUID. Accessible by SUPER_ADMIN.")
    @PutMapping("/{id}")
    public ResponseEntity<com.iptv.wiseplayer.dto.response.AdminResponse> updateAdmin(
            @PathVariable UUID id,
            @RequestBody com.iptv.wiseplayer.dto.request.UpdateAdminRequest request) {
        return ResponseEntity.ok(adminManagementService.updateAdmin(id, request));
    }

    @Operation(summary = "Toggle Admin Status", description = "Toggles active/inactive status of an admin by UUID. Accessible by SUPER_ADMIN.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<com.iptv.wiseplayer.dto.response.AdminResponse> toggleAdminStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(adminManagementService.toggleAdminStatus(id));
    }

    @Operation(summary = "Delete Admin", description = "Permanently deletes an admin account by UUID. Accessible by SUPER_ADMIN.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAdmin(@PathVariable UUID id) {
        adminManagementService.deleteAdmin(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Admin deleted successfully"
        ));
    }
}

