package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.AdminRole;

import java.time.LocalDateTime;
import java.util.UUID;

public class AdminPermissionViewDTO {

    private UUID adminId;
    private String email;
    private String username;
    private AdminRole role;
    private boolean canCreate;
    private boolean canRead;
    private boolean canUpdate;
    private boolean canDelete;
    private LocalDateTime updatedAt;
    private boolean recentlyChangedByMe;

    public AdminPermissionViewDTO() {}

    public UUID getAdminId() { return adminId; }
    public void setAdminId(UUID adminId) { this.adminId = adminId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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
