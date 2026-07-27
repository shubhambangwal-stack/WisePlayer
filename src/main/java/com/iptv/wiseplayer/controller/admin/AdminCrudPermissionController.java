package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest;
import com.iptv.wiseplayer.service.AdminResellerService;
import com.iptv.wiseplayer.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Unified CRUD permission management controller with consistent URL patterns:
 *
 *   PUT   /api/admin/crud-permissions/bulk/{role}  → change ALL admins of that role + sync defaults
 *   PATCH /api/admin/crud-permissions/{id}         → change a SINGLE admin's flags by UUID
 *
 * Hierarchy rules (enforced in service layer):
 *   SUPER_ADMIN can manage → ADMIN, RESELLER, SUB_RESELLER
 *   ADMIN       can manage → RESELLER, SUB_RESELLER
 *   (RESELLER manages SUB_RESELLER via /api/reseller/crud-permissions/*)
 *
 * Escalation rule: caller cannot grant flags they do not themselves possess.
 */
@RestController
@RequestMapping("/api/admin/crud-permissions")
@Tag(name = "Admin CRUD Permissions",
     description = "Change CRUD flags for all admins of a role (bulk) or a specific admin by ID")
public class AdminCrudPermissionController {

    private final AdminResellerService adminResellerService;
    private final RolePermissionService rolePermissionService;

    public AdminCrudPermissionController(AdminResellerService adminResellerService,
                                         RolePermissionService rolePermissionService) {
        this.adminResellerService = adminResellerService;
        this.rolePermissionService = rolePermissionService;
    }

    @Operation(
        summary = "View CRUD permissions for lower roles",
        description = "Returns the CRUD permissions for all lower roles and lower-ranking individual admins, including a flag indicating if the permission was recently changed by the caller."
    )
    @GetMapping
    public ResponseEntity<com.iptv.wiseplayer.dto.response.CrudPermissionViewResponse> getCrudPermissions() {
        return ResponseEntity.ok(adminResellerService.getCrudPermissionsView());
    }

    // -------------------------------------------------------------------------
    // BULK — change ALL admins of a role
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Bulk: update CRUD for ALL admins of a role",
        description = "Updates canCreate/canRead/canUpdate/canDelete for EVERY existing admin of the given role, " +
                      "AND updates the role_permissions defaults table so future registrations inherit the same flags. " +
                      "Hierarchy rule: caller must outrank the target role. " +
                      "Escalation rule: caller cannot grant flags they don't own. " +
                      "Null fields are skipped (partial update). " +
                      "Valid roles: ADMIN, RESELLER, SUB_RESELLER."
    )
    @PutMapping("/bulk/{role}")
    public ResponseEntity<?> bulkUpdateByRole(
            @PathVariable AdminRole role,
            @RequestBody UpdateRolePermissionRequest request) {

        // Delegate to RolePermissionService which handles hierarchy check + dual sync
        // (role_permissions table + all existing admins of that role)
        rolePermissionService.updateRoleDefaults(role, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "CRUD permissions updated for all existing " + role.name() +
                           " accounts and saved as new default for future registrations."));
    }

    // -------------------------------------------------------------------------
    // INDIVIDUAL — change a single admin by UUID
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Individual: update CRUD for a specific admin by ID",
        description = "Changes only the CRUD flags for a single admin identified by their UUID. " +
                      "Hierarchy rule: caller must outrank the target admin. " +
                      "Escalation rule: caller cannot grant flags they don't own. " +
                      "Null fields are skipped (partial update). " +
                      "Works for any target role (ADMIN, RESELLER, SUB_RESELLER)."
    )
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateByAdminId(
            @PathVariable UUID id,
            @RequestBody UpdateRolePermissionRequest request) {

        adminResellerService.updateAdminPermissionsById(id, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "CRUD permissions updated for admin " + id));
    }
}
