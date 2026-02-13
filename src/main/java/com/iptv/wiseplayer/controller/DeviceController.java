package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.DeviceValidationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.DeviceValidationResponse;
import com.iptv.wiseplayer.dto.request.DeviceActivationRequest;
import com.iptv.wiseplayer.dto.response.DeviceActivationResponse;
import com.iptv.wiseplayer.dto.request.DeviceKeyRequest;
import com.iptv.wiseplayer.dto.response.DeviceKeyResponse;
import com.iptv.wiseplayer.dto.response.DeviceKeyStatusResponse;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.DeviceKeyService;
import com.iptv.wiseplayer.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for device management.
 * Exposes endpoints for device registration, validation, and activation keys.
 */
@RestController
@RequestMapping("/api/device")
@Tag(name = "Device Management", description = "APIs for device registration, validation, activation, and key management")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceKeyService deviceKeyService;
    private final DeviceContext deviceContext;

    public DeviceController(DeviceService deviceService, DeviceKeyService deviceKeyService,
            DeviceContext deviceContext) {
        this.deviceService = deviceService;
        this.deviceKeyService = deviceKeyService;
        this.deviceContext = deviceContext;
    }

    /**
     * Register a new device.
     */
    @Operation(summary = "Register Device", description = "Registers a new device using its unique identifier and metadata.")
    @PostMapping("/register")
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(@RequestBody DeviceRegistrationRequest request) {
        DeviceRegistrationResponse response = deviceService.registerDevice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate a device on app launch.
     */
    @Operation(summary = "Validate Device", description = "Validates the device status on application launch to ensure it is authorized.")
    @PostMapping("/validate")
    public ResponseEntity<DeviceValidationResponse> validateDevice(@RequestBody DeviceValidationRequest request) {
        DeviceValidationResponse response = deviceService.validateDevice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate a 6-digit numeric activation key for a device.
     */
    @Operation(summary = "Generate Activation Key", description = "Generates a temporary 6-digit numeric key for device activation.")
    @PostMapping("/key")
    public ResponseEntity<DeviceKeyResponse> generateKey(@RequestBody DeviceKeyRequest request) {
        DeviceKeyResponse response = deviceKeyService.generateDeviceKey(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate a device using the 6-digit code.
     */
    @Operation(summary = "Activate Device", description = "Activates a device using the provided 6-digit activation code.")
    @PostMapping("/activate")
    public ResponseEntity<DeviceActivationResponse> activateDevice(@RequestBody DeviceActivationRequest request) {
        DeviceActivationResponse response = deviceKeyService.activateDevice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Check the activation status of a device key.
     * Uses the authenticated device context.
     */
    @Operation(summary = "Get Key Status", description = "Checks the status of the activation key for the authenticated device.")
    @GetMapping("/key/status")
    public ResponseEntity<DeviceKeyStatusResponse> getKeyStatus() {
        DeviceKeyStatusResponse response = deviceKeyService.getKeyStatus(deviceContext.getCurrentDeviceId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh Token", description = "Refreshes the device access token using the hardware-linked secret.")
    @PostMapping("/refresh")
    public ResponseEntity<DeviceValidationResponse> refreshToken(
            @RequestHeader("X-Device-Secret") String deviceSecret,
            @RequestHeader("X-Device-Fingerprint") String fingerprint) {
        DeviceValidationResponse response = deviceService.refreshDeviceToken(deviceSecret, fingerprint);
        return ResponseEntity.ok(response);
    }
}
