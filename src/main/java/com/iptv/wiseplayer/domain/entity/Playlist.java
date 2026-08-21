package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.domain.enums.OwnerType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "playlists", indexes = {
        @Index(name = "idx_playlist_device_id", columnList = "device_id")
})
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "playlist_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "device_id")
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 30)
    private OwnerType ownerType;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 100 , columnDefinition = "VARCHAR(100) DEFAULT 'My Playlist'")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PlaylistType type;




    // Encrypted Fields
    @Column(name = "server_url", columnDefinition = "TEXT")
    private String serverUrl;

    @Column(name = "username", length = 512)
    private String username;

    @Column(name = "password", length = 512)
    private String password;

    @Column(name = "m3u_url", columnDefinition = "TEXT")
    private String m3uUrl;

    @Column(name = "pinned", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean pinned = false;

    @Column(name = "pin_hash", length = 100)
    private String pinHash;

    // Catch-up / Archive capability snapshot (cached per playlist to reduce API calls)
    @Column(name = "catchup_supported", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean catchupSupported = false;

    @Column(name = "catchup_method", length = 20)
    private String catchupMethod;

    @Column(name = "catchup_days")
    private Integer catchupDays;

    @Column(name = "catchup_source", columnDefinition = "TEXT")
    private String catchupSource;

    @Column(name = "catchup_checked_at")
    private LocalDateTime catchupCheckedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Playlist() {
    }

    // Constructor for Xtream
    public Playlist(UUID deviceId, String name, String serverUrl, String username, String password) {
        this.deviceId = deviceId;
        this.name = name;
        this.type = PlaylistType.XTREAM;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
    }

    // Constructor for M3U
    public Playlist(UUID deviceId, String name, String m3uUrl) {
        this.deviceId = deviceId;
        this.name = name;
        this.type = PlaylistType.M3U;
        this.m3uUrl = m3uUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public OwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(OwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public PlaylistType getType() {
        return type;
    }

    public void setType(PlaylistType type) {
        this.type = type;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getM3uUrl() {
        return m3uUrl;
    }

    public void setM3uUrl(String m3uUrl) {
        this.m3uUrl = m3uUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isCatchupSupported() {
        return catchupSupported;
    }

    public void setCatchupSupported(boolean catchupSupported) {
        this.catchupSupported = catchupSupported;
    }

    public String getCatchupMethod() {
        return catchupMethod;
    }

    public void setCatchupMethod(String catchupMethod) {
        this.catchupMethod = catchupMethod;
    }

    public Integer getCatchupDays() {
        return catchupDays;
    }

    public void setCatchupDays(Integer catchupDays) {
        this.catchupDays = catchupDays;
    }

    public String getCatchupSource() {
        return catchupSource;
    }

    public void setCatchupSource(String catchupSource) {
        this.catchupSource = catchupSource;
    }

    public LocalDateTime getCatchupCheckedAt() {
        return catchupCheckedAt;
    }

    public void setCatchupCheckedAt(LocalDateTime catchupCheckedAt) {
        this.catchupCheckedAt = catchupCheckedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }
}
