package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.DeviceActivationRequest;
import com.iptv.wiseplayer.dto.request.DeviceKeyRequest;
import com.iptv.wiseplayer.dto.response.DeviceActivationResponse;
import com.iptv.wiseplayer.dto.response.DeviceKeyResponse;
import com.iptv.wiseplayer.dto.response.DeviceKeyStatusResponse;

import java.util.UUID;

/**
 * Service interface for managing device activation keys.
 */
public interface DeviceKeyService {

    /**
     * Generate a new activation key for a device.
     * Invalidates any existing keys for the device.
     *
     * @param request Request containing device ID
     * @return Response containing the raw activation key (one-time view)
     */
    DeviceKeyResponse generateDeviceKey(DeviceKeyRequest request);

    /**
     * Activate a device using a key.
     * Validates device ID and key hash match.
     *
     * @param request Request containing device ID and activation key
     * @return Activation status
     */
    DeviceActivationResponse activateDevice(DeviceActivationRequest request);

    /**
     * Check the status of the most recent key for a device.
     *
     * @param deviceId Internal Device UUID
     * @return Status response
     */
    DeviceKeyStatusResponse getKeyStatus(UUID deviceId);
}
