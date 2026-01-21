package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.PlaylistType;

import java.util.UUID;

public class PlaylistResponse {
    private UUID id;
    private UUID deviceId;
    private String name;
    private PlaylistType type;
    private String serverUrl;
    private String username;
    private String password;
    private String m3uUrl;

    public PlaylistResponse() {
    }

    public PlaylistResponse(UUID id, UUID deviceId, String name, PlaylistType type, String serverUrl, String username,
            String password, String m3uUrl) {
        this.id = id;
        this.deviceId = deviceId;
        this.name = name;
        this.type = type;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getM3uUrl() {
        return m3uUrl;
    }

    public void setM3uUrl(String m3uUrl) {
        this.m3uUrl = m3uUrl;
    }
}
