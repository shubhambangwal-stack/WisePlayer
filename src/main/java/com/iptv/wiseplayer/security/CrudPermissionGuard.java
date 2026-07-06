package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.dto.request.UpdateResellerRequest;
import com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * =====================================================================
 * CENTRAL PERMISSION ESCALATION GUARD
 * =====================================================================
 *
 * Centralizes the rule: "A caller cannot GRANT a permission flag
 * to another admin if they do not themselves possess that flag."
 *
 * Previously this 4-line if-block was copy-pasted 6 times across
 * service files. Now it lives here once.
 *
 * Usage:
 *   crudPermissionGuard.checkEscalation(callerAdmin, request);
 *
 * SuperAdmin callers should NOT call this — they can grant anything.
 * =====================================================================
 */
@Component
public class CrudPermissionGuard {

    private static final String ESCALATION_MSG =
            "You cannot grant permissions that you do not yourself possess.";

    /**
     * Checks that {@code caller} is not trying to grant a flag it doesn't own.
     * Null values in the request mean "don't change" and are ignored.
     *
     * @param caller    the Admin performing the assignment
     * @param canCreate nullable; the CREATE flag being assigned
     * @param canRead   nullable; the READ flag being assigned
     * @param canUpdate nullable; the UPDATE flag being assigned
     * @param canDelete nullable; the DELETE flag being assigned
     * @throws AccessDeniedException if any flag being granted exceeds caller's own flags
     */
    public void checkEscalation(Admin caller,
                                Boolean canCreate,
                                Boolean canRead,
                                Boolean canUpdate,
                                Boolean canDelete) {
        if ((canCreate != null && canCreate && !caller.isCanCreate()) ||
            (canRead   != null && canRead   && !caller.isCanRead())   ||
            (canUpdate != null && canUpdate && !caller.isCanUpdate()) ||
            (canDelete != null && canDelete && !caller.isCanDelete())) {
            throw new AccessDeniedException(ESCALATION_MSG);
        }
    }

    /**
     * Convenience overload for {@link UpdateRolePermissionRequest}.
     */
    public void checkEscalation(Admin caller, UpdateRolePermissionRequest request) {
        checkEscalation(caller,
                request.getCanCreate(),
                request.getCanRead(),
                request.getCanUpdate(),
                request.getCanDelete());
    }

    /**
     * Convenience overload for {@link UpdateResellerRequest}.
     */
    public void checkEscalation(Admin caller, UpdateResellerRequest request) {
        checkEscalation(caller,
                request.getCanCreate(),
                request.getCanRead(),
                request.getCanUpdate(),
                request.getCanDelete());
    }
}
