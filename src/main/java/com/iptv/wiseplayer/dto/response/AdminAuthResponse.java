package com.iptv.wiseplayer.dto.response;

public class AdminAuthResponse {
    private boolean success;
    private String token;
    private String email;
    private String username;
    private String fullName;
    private String role;

    public AdminAuthResponse() {}

    public AdminAuthResponse(boolean success, String token, String email,
                             String username, String fullName, String role) {
        this.success = success;
        this.token = token;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
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
}
