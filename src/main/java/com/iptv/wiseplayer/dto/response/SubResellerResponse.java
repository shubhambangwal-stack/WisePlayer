package com.iptv.wiseplayer.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class SubResellerResponse {
    private UUID id;
    private String username;
    private String fullName;
    private long activeUsers;
    private String status;
    private LocalDateTime joinedAt;

    public SubResellerResponse() {}

    public SubResellerResponse(UUID id, String username, String fullName, long activeUsers, String status, LocalDateTime joinedAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.activeUsers = activeUsers;
        this.status = status;
        this.joinedAt = joinedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public static SubResellerResponseBuilder builder() {
        return new SubResellerResponseBuilder();
    }

    public static class SubResellerResponseBuilder {
        private UUID id;
        private String username;
        private String fullName;
        private long activeUsers;
        private String status;
        private LocalDateTime joinedAt;

        public SubResellerResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public SubResellerResponseBuilder username(String username) {
            this.username = username;
            return this;
        }
        public SubResellerResponseBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }
        public SubResellerResponseBuilder activeUsers(long activeUsers) {
            this.activeUsers = activeUsers;
            return this;
        }
        public SubResellerResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public SubResellerResponseBuilder joinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
            return this;
        }
        public SubResellerResponse build() {
            return new SubResellerResponse(id, username, fullName, activeUsers, status, joinedAt);
        }
    }
}
