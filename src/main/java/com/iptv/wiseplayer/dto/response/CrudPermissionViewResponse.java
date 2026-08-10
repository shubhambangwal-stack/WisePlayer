package com.iptv.wiseplayer.dto.response;

import java.util.List;

public class CrudPermissionViewResponse {

    private List<RolePermissionViewDTO> rolePermissions;
    private List<AdminPermissionViewDTO> individualPermissions;

    public CrudPermissionViewResponse() {}

    public CrudPermissionViewResponse(List<RolePermissionViewDTO> rolePermissions, List<AdminPermissionViewDTO> individualPermissions) {
        this.rolePermissions = rolePermissions;
        this.individualPermissions = individualPermissions;
    }

    public List<RolePermissionViewDTO> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(List<RolePermissionViewDTO> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    public List<AdminPermissionViewDTO> getIndividualPermissions() {
        return individualPermissions;
    }

    public void setIndividualPermissions(List<AdminPermissionViewDTO> individualPermissions) {
        this.individualPermissions = individualPermissions;
    }
}
