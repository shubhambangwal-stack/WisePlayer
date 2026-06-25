package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * =====================================================================
 * CENTRAL CRUD PERMISSION ENFORCEMENT POINT
 * =====================================================================
 *
 * This aspect fires BEFORE any service method annotated with
 * {@link RequiresCrud} and checks whether the current authenticated
 * user has the required CRUD flag enabled on their Admin record.
 *
 * Rules:
 *  - SuperAdmin  → always permitted (no flag check)
 *  - Admin/Reseller/SubReseller → their canCreate/canRead/canUpdate/canDelete
 *    flag must be TRUE for the corresponding operation
 *  - Unauthenticated → 403 (security filter should catch this first,
 *    but defended here as well)
 *
 * To change CRUD enforcement logic in the future, edit ONLY this class.
 * =====================================================================
 */
@Aspect
@Component
public class CrudPermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(CrudPermissionAspect.class);

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;

    public CrudPermissionAspect(AdminRepository adminRepository,
                                SuperAdminRepository superAdminRepository) {
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    /**
     * Intercepts any method annotated with {@code @RequiresCrud} and
     * validates that the current caller has the required permission flag.
     *
     * @param requiresCrud the annotation carrying the required {@link CrudOperation}
     * @throws AccessDeniedException if the caller lacks the required flag
     */
    @Before("@annotation(requiresCrud)")
    public void enforce(RequiresCrud requiresCrud) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required.");
        }

        String username = auth.getName();

        // SuperAdmin bypasses all CRUD flag checks
        if (superAdminRepository.findByUsername(username).isPresent()) {
            return;
        }

        // Look up the Admin's CRUD flags
        Admin caller = adminRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Caller not found: " + username));

        CrudOperation operation = requiresCrud.value();
        boolean permitted = switch (operation) {
            case CREATE -> caller.isCanCreate();
            case READ   -> caller.isCanRead();
            case UPDATE -> caller.isCanUpdate();
            case DELETE -> caller.isCanDelete();
        };

        if (!permitted) {
            log.warn("CRUD permission denied: user={} role={} operation={}",
                    username, caller.getRole(), operation);
            throw new AccessDeniedException(
                    "You do not have " + operation.name() + " permission.");
        }

        log.debug("CRUD permission granted: user={} operation={}", username, operation);
    }
}
