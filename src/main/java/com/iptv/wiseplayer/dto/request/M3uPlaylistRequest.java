package com.iptv.wiseplayer.dto.request;

public class M3uPlaylistRequest {

    private String deviceId; // Fingerprint
    private String m3uUrl;

    public M3uPlaylistRequest() {
    }

    public M3uPlaylistRequest(String deviceId, String m3uUrl) {
        this.deviceId = deviceId;
        this.m3uUrl = m3uUrl;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getM3uUrl() {
        return m3uUrl;
    }

    public void setM3uUrl(String m3uUrl) {
        this.m3uUrl = m3uUrl;
    }
}
