package com.iptv.wiseplayer.dto.request;

/**
 * Request body for updating the default CRUD flags for a role.
 * Null fields are ignored (partial update).
 */
public class UpdateRolePermissionRequest {

    private Boolean canCreate;
    private Boolean canRead;
    private Boolean canUpdate;
    private Boolean canDelete;

    public UpdateRolePermissionRequest() {}

    public Boolean getCanCreate() { return canCreate; }
    public void setCanCreate(Boolean canCreate) { this.canCreate = canCreate; }

    public Boolean getCanRead() { return canRead; }
    public void setCanRead(Boolean canRead) { this.canRead = canRead; }

    public Boolean getCanUpdate() { return canUpdate; }
    public void setCanUpdate(Boolean canUpdate) { this.canUpdate = canUpdate; }

    public Boolean getCanDelete() { return canDelete; }
    public void setCanDelete(Boolean canDelete) { this.canDelete = canDelete; }
}
