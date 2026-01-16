package com.iptv.wiseplayer.dto.request;

public class XtreamPlaylistRequest {

    private String deviceId; // Fingerprint
    private String serverUrl;
    private String username;
    private String password;

    public XtreamPlaylistRequest() {
    }

    public XtreamPlaylistRequest(String deviceId, String serverUrl, String username, String password) {
        this.deviceId = deviceId;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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
}
