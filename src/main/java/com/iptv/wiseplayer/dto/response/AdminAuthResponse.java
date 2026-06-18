package com.iptv.wiseplayer.dto.response;

public class AdminAuthResponse {
    private boolean success;
    private String token;
    private String email;
    private String username;
    private String fullName;
    private String role;
    private boolean canCreate;
    private boolean canRead;
    private boolean canUpdate;
    private boolean canDelete;

    public AdminAuthResponse() {}

    public AdminAuthResponse(boolean success, String token, String email,
                             String username, String fullName, String role,
                             boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.success = success;
        this.token = token;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isCanCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }

    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }

    public boolean isCanUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
