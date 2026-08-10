package com.iptv.wiseplayer.dto.request;

import com.iptv.wiseplayer.domain.enums.PlaylistType;

/**
 * Request DTO for updating an existing playlist.
 * All fields are optional; only non-null values will be applied.
 */
public class UpdatePlaylistRequest {

    private String name;

    // Xtream fields (only used when type = XTREAM)
    private String serverUrl;
    private String username;
    private String password;

    // M3U field (only used when type = M3U)
    private String m3uUrl;

    public UpdatePlaylistRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
