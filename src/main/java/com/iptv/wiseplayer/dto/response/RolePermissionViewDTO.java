package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.AdminRole;

import java.time.LocalDateTime;

public class RolePermissionViewDTO {

    private AdminRole role;
    private boolean canCreate;
    private boolean canRead;
    private boolean canUpdate;
    private boolean canDelete;
    private LocalDateTime updatedAt;
    private boolean recentlyChangedByMe;

    public RolePermissionViewDTO() {}

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

    public boolean isRecentlyChangedByMe() { return recentlyChangedByMe; }
    public void setRecentlyChangedByMe(boolean recentlyChangedByMe) { this.recentlyChangedByMe = recentlyChangedByMe; }
}
