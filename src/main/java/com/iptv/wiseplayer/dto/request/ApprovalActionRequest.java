package com.iptv.wiseplayer.dto.request;

public class ApprovalActionRequest {
    private String adminNotes;

    public ApprovalActionRequest() {}

    public ApprovalActionRequest(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
