package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

public class ResellerResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private AdminRole role;
    private boolean active;
    private LocalDateTime createdAt;
    private long totalUsers;
    private BigDecimal credits;

    public ResellerResponse() {}

    public ResellerResponse(UUID id, String username, String fullName, String email, AdminRole role, 
                            boolean active, LocalDateTime createdAt, long totalUsers, BigDecimal credits) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.totalUsers = totalUsers;
        this.credits = credits;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public AdminRole getRole() { return role; }
    public void setRole(AdminRole role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }
}
