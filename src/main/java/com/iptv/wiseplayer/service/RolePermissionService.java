package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.RolePermission;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest;
import com.iptv.wiseplayer.dto.response.RolePermissionResponse;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.RolePermissionRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iptv.wiseplayer.security.CrudOperation;
import com.iptv.wiseplayer.security.CrudPermissionGuard;
import com.iptv.wiseplayer.security.RequiresCrud;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the role_permissions table which stores default CRUD flags per AdminRole.
 *
 * When a new reseller / sub-reseller is registered these defaults are applied to their
 * admin record. When the defaults are updated, ALL existing admins of that role are
 * also updated (via AdminRepository.updatePermissionsByRole) so existing and future
 * admins always stay consistent.
 */
@Service
public class RolePermissionService {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionService.class);

    private final RolePermissionRepository rolePermissionRepository;
    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final CrudPermissionGuard crudPermissionGuard;

    public RolePermissionService(RolePermissionRepository rolePermissionRepository,
                                 AdminRepository adminRepository,
                                 SuperAdminRepository superAdminRepository,
                                 CrudPermissionGuard crudPermissionGuard) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
        this.crudPermissionGuard = crudPermissionGuard;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /**
     * Returns the default permission flags for a given role.
     * Falls back to all-true if no row exists yet.
     */
    public RolePermissionResponse getRoleDefaults(AdminRole role) {
        RolePermission rp = rolePermissionRepository.findByRole(role)
                .orElse(RolePermission.allTrue(role));
        return toResponse(rp);
    }

    /**
     * Returns defaults for all four roles.
     */
    public List<RolePermissionResponse> getAllRoleDefaults() {
        return Arrays.stream(AdminRole.values())
                .map(role -> rolePermissionRepository.findByRole(role)
                        .orElse(RolePermission.allTrue(role)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convenience method used by registration flows to get the raw entity.
     * Returns an all-true fallback entity if the row is missing.
     */
    public RolePermission getDefaultsForRole(AdminRole role) {
        return rolePermissionRepository.findByRole(role)
                .orElse(RolePermission.allTrue(role));
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    /**
     * Updates the default flags for a role in role_permissions table,
     * AND applies the same change to ALL existing admins of that role
     * so that existing and future admins remain consistent.
     *
     * Hierarchy enforced: caller must outrank the target role.
     * Escalation enforced: caller cannot grant flags they don't own.
     * Null values are treated as "no change".
     */
    @Transactional
    @RequiresCrud(CrudOperation.UPDATE)
    public RolePermissionResponse updateRoleDefaults(AdminRole role, UpdateRolePermissionRequest request) {
        // --- Hierarchy & escalation check ---
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isSuperAdmin = superAdminRepository.findByUsername(currentUsername).isPresent();

        if (!isSuperAdmin) {
            Admin caller = adminRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));

            // Caller must outrank the target role
            if (!caller.getRole().canManage(role)) {
                throw new AccessDeniedException(
                        "You cannot modify permissions for role " + role.name() +
                        ". You must be higher-ranked than that role.");
            }

            // Centralized escalation check
            crudPermissionGuard.checkEscalation(caller, request);
        }

        // --- Upsert role_permissions row ---
        RolePermission rp = rolePermissionRepository.findByRole(role)
                .orElseGet(() -> RolePermission.allTrue(role));

        if (request.getCanCreate() != null) rp.setCanCreate(request.getCanCreate());
        if (request.getCanRead()   != null) rp.setCanRead(request.getCanRead());
        if (request.getCanUpdate() != null) rp.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) rp.setCanDelete(request.getCanDelete());

        rolePermissionRepository.save(rp);
        log.info("Updated role_permissions for role={}: create={} read={} update={} delete={}",
                role, rp.isCanCreate(), rp.isCanRead(), rp.isCanUpdate(), rp.isCanDelete());

        // --- Apply same change to ALL existing admins of this role ---
        adminRepository.updatePermissionsByRole(
                role,
                request.getCanCreate(),
                request.getCanRead(),
                request.getCanUpdate(),
                request.getCanDelete());
        log.info("Bulk-updated existing admins with role={} to match new defaults", role);

        return toResponse(rp);
    }

    // -------------------------------------------------------------------------
    // SEED (called by DataInitializer)
    // -------------------------------------------------------------------------

    /**
     * Seeds default rows for all roles if they do not exist yet.
     * Safe to call multiple times (idempotent).
     */
    @Transactional
    public void seedDefaults() {
        for (AdminRole role : AdminRole.values()) {
            if (rolePermissionRepository.findByRole(role).isEmpty()) {
                RolePermission rp = RolePermission.allTrue(role);
                rolePermissionRepository.save(rp);
                log.info("Seeded default role_permissions row for role={}", role);
            }
        }
    }

    // -------------------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------------------

    private RolePermissionResponse toResponse(RolePermission rp) {
        return new RolePermissionResponse(
                rp.getRole(),
                rp.isCanCreate(),
                rp.isCanRead(),
                rp.isCanUpdate(),
                rp.isCanDelete(),
                rp.getUpdatedAt());
    }
}
