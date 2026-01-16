package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.PlaylistType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "playlists", indexes = {
        @Index(name = "idx_playlist_device_id", columnList = "device_id", unique = true)
})
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "playlist_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "device_id", nullable = false, unique = true)
    private UUID deviceId;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Playlist() {
    }

    // Constructor for Xtream
    public Playlist(UUID deviceId, String serverUrl, String username, String password) {
        this.deviceId = deviceId;
        this.type = PlaylistType.XTREAM;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
    }

    // Constructor for M3U
    public Playlist(UUID deviceId, String m3uUrl) {
        this.deviceId = deviceId;
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
}
