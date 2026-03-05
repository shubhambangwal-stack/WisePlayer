package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.dto.request.AdminLoginRequest;
import com.iptv.wiseplayer.dto.request.CreateAdminRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminTokenUtil adminTokenUtil;

    public AdminAuthService(AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            AdminTokenUtil adminTokenUtil) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminTokenUtil = adminTokenUtil;
    }

    public AdminAuthResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!admin.isActive()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = adminTokenUtil.generateToken(admin.getUsername(), admin.getRole());

        return new AdminAuthResponse(true, token, admin.getUsername(), admin.getFullName());
    }

    public Map<String, Object> createAdmin(CreateAdminRequest request) {
        if (adminRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setFullName(request.getFullName());
        admin.setRole(request.getRole() != null ? request.getRole() : "ADMIN");
        admin.setActive(true);

        adminRepository.save(admin);

        return Map.of(
                "success", true,
                "message", "Admin account created. Please login via /api/admin/auth/login",
                "username", admin.getUsername());
    }
}
