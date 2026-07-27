package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admins", indexes = {
        @Index(name = "idx_admin_username", columnList = "username"),
        @Index(name = "idx_admin_email", columnList = "email")
})
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "admin_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AdminRole role = AdminRole.ADMIN;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "creator_id")
    private UUID creatorId;

    @Column(name = "credits", precision = 10, scale = 2, nullable = false)
    private BigDecimal credits = BigDecimal.ZERO;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = true;

    @Column(name = "can_read", nullable = false)
    private boolean canRead = true;

    @Column(name = "can_update", nullable = false)
    private boolean canUpdate = true;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = true;

    @Column(name = "partner_level", length = 20)
    private String partnerLevel = "SILVER";

    public Admin() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public AdminRole getRole() { return role; }
    public void setRole(AdminRole role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public UUID getCreatorId() { return creatorId; }
    public void setCreatorId(UUID creatorId) { this.creatorId = creatorId; }
    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }
    public String getPartnerLevel() { return partnerLevel; }
    public void setPartnerLevel(String partnerLevel) { this.partnerLevel = partnerLevel; }

    public boolean isSuperAdmin() {
        return AdminRole.SUPER_ADMIN.equals(this.role);
    }

    public boolean isCanCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }
    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }
    public boolean isCanUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }
    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
