package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;

public class XtreamPlaylistRequest {

    @NotBlank(message = "Playlist name is required")
    private String name;

    @NotBlank(message = "Server URL is required")
    private String serverUrl;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    public XtreamPlaylistRequest() {
    }

    public XtreamPlaylistRequest(String name, String serverUrl, String username, String password) {
        this.name = name;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
