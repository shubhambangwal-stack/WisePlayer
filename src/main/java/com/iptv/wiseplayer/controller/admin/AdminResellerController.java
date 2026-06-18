package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.response.ResellerResponse;
import com.iptv.wiseplayer.service.AdminResellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/resellers")
@Tag(name = "Admin Reseller Management", description = "Endpoints for managing resellers and sub-resellers")
public class AdminResellerController {

    private final AdminResellerService adminResellerService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AdminResellerController(AdminResellerService adminResellerService,
                                 org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.adminResellerService = adminResellerService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "List All Resellers", description = "Retrieves a paginated list of all resellers and sub-resellers.")
    @GetMapping
    public ResponseEntity<Page<ResellerResponse>> getAllResellers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            Pageable pageable) {
        return ResponseEntity.ok(adminResellerService.getAllResellers(username, fullName, email, pageable));
    }

    @Operation(summary = "Get Reseller Details", description = "Retrieves detailed information for a specific reseller.")
    @GetMapping("/{id}")
    public ResponseEntity<ResellerResponse> getResellerById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminResellerService.getResellerById(id));
    }

    @Operation(summary = "Update Reseller Details", description = "Updates a reseller's information. Only accessible by SUPER_ADMIN.")
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> updateReseller(@PathVariable UUID id, @RequestBody com.iptv.wiseplayer.dto.request.UpdateResellerRequest request) {
        adminResellerService.updateReseller(id, request, passwordEncoder);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reseller updated successfully"));
    }

    @Operation(summary = "Toggle Reseller Status", description = "Activates or deactivates a reseller account.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleResellerStatus(@PathVariable UUID id) {
        adminResellerService.toggleResellerStatus(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reseller status toggled successfully"));
    }

    @Operation(summary = "Get Reseller Stats", description = "Retrieves KPI statistics for a reseller.")
    @GetMapping("/{id}/stats")
    public ResponseEntity<com.iptv.wiseplayer.dto.response.ResellerStatsResponse> getResellerStats(@PathVariable UUID id) {
        return ResponseEntity.ok(adminResellerService.getResellerStats(id));
    }

    @Operation(summary = "Get Reseller Analytics", description = "Retrieves time-series data for activations and revenue.")
    @GetMapping("/{id}/analytics")
    public ResponseEntity<List<com.iptv.wiseplayer.dto.response.ResellerAnalyticsResponse>> getResellerAnalytics(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "WEEK") String period,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate) {
        return ResponseEntity.ok(adminResellerService.getResellerAnalytics(id, period, startDate));
    }

    @Operation(summary = "Get Sub-Resellers", description = "List sub-resellers managed by this reseller.")
    @GetMapping("/{id}/sub-resellers")
    public ResponseEntity<Page<com.iptv.wiseplayer.dto.response.SubResellerResponse>> getSubResellers(
            @PathVariable UUID id,
            Pageable pageable) {
        return ResponseEntity.ok(adminResellerService.getSubResellers(id, pageable));
    }
    @Operation(summary = "Delete Reseller", description = "Permanently deletes a reseller and all associated data.")
    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteReseller(@PathVariable UUID id) {
        adminResellerService.deleteReseller(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reseller deleted successfully"));
    }

    @Operation(summary = "Bulk Update Role Permissions", description = "Updates permissions for all users in a specific role at once.")
    @PutMapping("/crud-permissions/{role}")
    public ResponseEntity<?> updateRolePermissions(
            @PathVariable com.iptv.wiseplayer.domain.enums.AdminRole role,
            @RequestBody com.iptv.wiseplayer.dto.request.UpdateResellerRequest request) {
        adminResellerService.updateRolePermissions(role, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Permissions updated successfully for all " + role.name() + "s"));
    }
}
