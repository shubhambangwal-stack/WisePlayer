package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.response.ResellerResponse;
import com.iptv.wiseplayer.service.AdminResellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/resellers")
@RequiredArgsConstructor
@Tag(name = "Admin Reseller Management", description = "Endpoints for managing resellers and sub-resellers")
public class AdminResellerController {

    private final AdminResellerService adminResellerService;

    @Operation(summary = "List All Resellers", description = "Retrieves a paginated list of all resellers and sub-resellers.")
    @GetMapping
    public ResponseEntity<Page<ResellerResponse>> getAllResellers(Pageable pageable) {
        return ResponseEntity.ok(adminResellerService.getAllResellers(pageable));
    }

    @Operation(summary = "Get Reseller Details", description = "Retrieves detailed information for a specific reseller.")
    @GetMapping("/{id}")
    public ResponseEntity<ResellerResponse> getResellerById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminResellerService.getResellerById(id));
    }

    @Operation(summary = "Toggle Reseller Status", description = "Activates or deactivates a reseller account.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleResellerStatus(@PathVariable UUID id) {
        adminResellerService.toggleResellerStatus(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reseller status toggled successfully"));
    }
}
