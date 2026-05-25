package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.dto.response.AdminDeviceResponse;
import com.iptv.wiseplayer.service.AdminDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/devices")
@Tag(name = "Admin Device Management", description = "Endpoints for managing registered devices")
public class AdminDeviceController {

    private final AdminDeviceService adminDeviceService;

    public AdminDeviceController(AdminDeviceService adminDeviceService) {
        this.adminDeviceService = adminDeviceService;
    }

    @Operation(summary = "List All Devices", description = "Retrieves a paginated list of all registered devices.")
    @GetMapping
    public ResponseEntity<Page<AdminDeviceResponse>> getAllDevices(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) SubscriptionType subscription,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String platform,
            Pageable pageable) {
        return ResponseEntity.ok(adminDeviceService.getAllDevices(deviceId, status, subscription, model, platform, pageable));
    }

    @Operation(summary = "Get Device Details", description = "Retrieves detailed information for a specific device.")
    @GetMapping("/{id}")
    public ResponseEntity<AdminDeviceResponse> getDeviceById(@PathVariable String id) {
        return ResponseEntity.ok(adminDeviceService.getDeviceByIdOrMac(id));
    }

    @Operation(summary = "Update Device Status", description = "Blocks or unblocks a device.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateDeviceStatus(@PathVariable String id, @RequestBody Map<String, String> payload) {
        DeviceStatus status = DeviceStatus.valueOf(payload.get("status").toUpperCase());
        adminDeviceService.updateDeviceStatus(id, status);
        return ResponseEntity.ok(Map.of("success", true, "message", "Device status updated to " + status));
    }

    @Operation(summary = "Delete Device Completely", description = "Permanently deletes a device and all its associated records.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeviceCompletely(@PathVariable String id) {
        adminDeviceService.deleteDeviceCompletely(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Device completely deleted."));
    }
}
