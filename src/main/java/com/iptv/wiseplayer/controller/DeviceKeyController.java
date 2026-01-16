package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.DeviceActivationRequest;
import com.iptv.wiseplayer.dto.request.DeviceKeyRequest;
import com.iptv.wiseplayer.dto.response.DeviceActivationResponse;
import com.iptv.wiseplayer.dto.response.DeviceKeyResponse;
import com.iptv.wiseplayer.dto.response.DeviceKeyStatusResponse;
import com.iptv.wiseplayer.service.DeviceKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
public class DeviceKeyController {

    private final DeviceKeyService deviceKeyService;

    public DeviceKeyController(DeviceKeyService deviceKeyService) {
        this.deviceKeyService = deviceKeyService;
    }

    /**
     * Generate a short-lived activation code for a device.
     * This code is displayed on the TV screen.
     */
    @PostMapping("/key")
    public ResponseEntity<DeviceKeyResponse> generateActivationKey(@RequestBody DeviceKeyRequest request) {
        DeviceKeyResponse response = deviceKeyService.generateDeviceKey(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate a device using the 6-digit code.
     * This is called by the mobile app or web portal.
     */
    @PostMapping("/activate")
    public ResponseEntity<DeviceActivationResponse> activateDevice(@RequestBody DeviceActivationRequest request) {
        DeviceActivationResponse response = deviceKeyService.activateDevice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Check activation status.
     * TV app polls this endpoint to detect when activation completes.
     */
    @GetMapping("/key/status")
    public ResponseEntity<DeviceKeyStatusResponse> getKeyStatus(@RequestParam String deviceId) {
        DeviceKeyStatusResponse response = deviceKeyService.getKeyStatus(deviceId);
        return ResponseEntity.ok(response);
    }
}
