package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AssignPlaylistRequest {
    
    @NotNull(message = "Device ID is required")
    private UUID deviceId;

    public AssignPlaylistRequest() {
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }
}
