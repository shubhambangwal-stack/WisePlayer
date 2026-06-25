package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores the DEFAULT CRUD permission flags for each AdminRole.
 * When a new admin/reseller/sub-reseller is created, their initial flags
 * are read from this table instead of being hardcoded to {@code true}.
 */
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AdminRole role;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = true;

    @Column(name = "can_read", nullable = false)
    private boolean canRead = true;

    @Column(name = "can_update", nullable = false)
    private boolean canUpdate = true;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RolePermission() {}

    /**
     * Factory method – produces an all-true fallback when no DB row exists.
     */
    public static RolePermission allTrue(AdminRole role) {
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setCanCreate(true);
        rp.setCanRead(true);
        rp.setCanUpdate(true);
        rp.setCanDelete(true);
        return rp;
    }

    // ---- Getters & Setters ----

    public AdminRole getRole() { return role; }
    public void setRole(AdminRole role) { this.role = role; }

    public boolean isCanCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }

    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }

    public boolean isCanUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
