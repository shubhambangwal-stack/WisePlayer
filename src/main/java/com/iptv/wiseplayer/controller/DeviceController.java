package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.DeviceValidationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.DeviceValidationResponse;
import com.iptv.wiseplayer.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for device management.
 * Exposes endpoints for device registration and validation.
 * No authentication required for these endpoints as they are used for initial
 * handshake.
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * Register a new device.
     * Idempotent: returns existing device if fingerprint matches.
     *
     * @param request Device registration details
     * @return Registered device information
     */
    @PostMapping("/register")
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(@RequestBody DeviceRegistrationRequest request) {
        DeviceRegistrationResponse response = deviceService.registerDevice(request);
        // Using CREATED (201) even if it existed, or could use OK (200) if strict
        // idempotency semantics desired.
        // Given requirements, returning OK is often safer for clients unless we
        // strictly track creations.
        // Let's use OK (200) since it might return an existing device.
        return ResponseEntity.ok(response);
    }

    /**
     * Validate a device on app launch.
     * Checks fingerprint and subscription status.
     *
     * @param request Device validation details
     * @return Validation status and access permission
     */
    @PostMapping("/validate")
    public ResponseEntity<DeviceValidationResponse> validateDevice(@RequestBody DeviceValidationRequest request) {
        DeviceValidationResponse response = deviceService.validateDevice(request);
        return ResponseEntity.ok(response);
    }
}
