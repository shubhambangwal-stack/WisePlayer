package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest;
import com.iptv.wiseplayer.dto.response.RolePermissionResponse;
import com.iptv.wiseplayer.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for managing per-role default CRUD permission flags.
 *
 * GET  /api/admin/role-permissions          – list defaults for all 4 roles
 * GET  /api/admin/role-permissions/{role}   – get defaults for one role
 * PUT  /api/admin/role-permissions/{role}   – update defaults + apply to existing admins (hierarchy-checked in service)
 */
@RestController
@RequestMapping("/api/admin/role-permissions")
@Tag(name = "Role Permission Defaults",
     description = "Manage default CRUD flags applied to new admins when they register")
public class AdminRolePermissionController {

    private final RolePermissionService rolePermissionService;

    public AdminRolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Operation(
        summary = "List all role defaults",
        description = "Returns the default CRUD permission flags for every AdminRole (SUPER_ADMIN, ADMIN, RESELLER, SUB_RESELLER)."
    )
    @GetMapping
    public ResponseEntity<List<RolePermissionResponse>> getAllRoleDefaults() {
        return ResponseEntity.ok(rolePermissionService.getAllRoleDefaults());
    }

    @Operation(
        summary = "Get defaults for a specific role",
        description = "Returns the default CRUD flags that are applied when a new admin of this role is created."
    )
    @GetMapping("/{role}")
    public ResponseEntity<RolePermissionResponse> getRoleDefaults(@PathVariable AdminRole role) {
        return ResponseEntity.ok(rolePermissionService.getRoleDefaults(role));
    }

    @Operation(
        summary = "Update role defaults",
        description = "Updates the default CRUD flags for the given role and instantly applies to ALL existing admins of that role. " +
                      "Hierarchy rule: caller must outrank the target role (SUPER_ADMIN > ADMIN > RESELLER > SUB_RESELLER). " +
                      "Escalation rule: caller cannot grant flags they do not themselves possess. " +
                      "Null fields are ignored (partial update)."
    )
    @PutMapping("/{role}")
    public ResponseEntity<?> updateRoleDefaults(
            @PathVariable AdminRole role,
            @RequestBody UpdateRolePermissionRequest request) {
        RolePermissionResponse updated = rolePermissionService.updateRoleDefaults(role, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Role defaults updated and applied to all existing " + role.name() + " accounts.",
                "data", updated));
    }
}
