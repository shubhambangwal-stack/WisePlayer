package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for assigning a playlist to a device.
 * Accepts either a standard UUID (e.g. "3e2b1c4d-...") or a MAC address
 * (colon-separated, e.g. "3E:B0:BD:00:DE:9B") in the {@code deviceId} field.
 */
public class AssignPlaylistRequest {

    private static final String UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final String MAC_REGEX =
            "^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$";

    @NotBlank(message = "Device ID or MAC address is required")
    @Pattern(
        regexp = UUID_REGEX + "|" + MAC_REGEX,
        message = "deviceId must be a valid UUID (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx) " +
                  "or a valid MAC address (XX:XX:XX:XX:XX:XX)"
    )
    private String deviceId;

    public AssignPlaylistRequest() {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns true if the provided deviceId looks like a MAC address.
     */
    public boolean isMacAddress() {
        return deviceId != null && deviceId.matches(MAC_REGEX);
    }
}
