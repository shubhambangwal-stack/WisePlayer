package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SubResellerUpdateRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String password;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
