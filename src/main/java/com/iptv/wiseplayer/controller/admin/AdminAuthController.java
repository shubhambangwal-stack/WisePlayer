package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.request.AdminLoginRequest;
import com.iptv.wiseplayer.dto.request.ChangePasswordRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Authentication", description = "Endpoints for administrator login and session management")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Admin Login", description = "Authenticates an administrator and returns a session token.")
    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> login(@RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }

    @Operation(summary = "Change Admin Password", description = "Allows an authenticated admin or superadmin to change their password.")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, 
            Principal principal) {
        
        adminAuthService.changePassword(principal.getName(), request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password changed successfully");
        
        return ResponseEntity.ok(response);
    }
}
