package com.iptv.wiseplayer.domain.enums;

/**
 * Roles for administrative access control.
 */
public enum AdminRole {
    SUPER_ADMIN(4),
    ADMIN(3),
    RESELLER(2),
    SUB_RESELLER(1);

    private final int rank;

    AdminRole(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public boolean canManage(AdminRole targetRole) {
        return this.rank > targetRole.getRank();
    }
}
