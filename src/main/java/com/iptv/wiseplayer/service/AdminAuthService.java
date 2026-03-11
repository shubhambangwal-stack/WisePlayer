package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.request.AdminLoginRequest;
import com.iptv.wiseplayer.dto.request.CreateAdminRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminTokenUtil adminTokenUtil;

    public AdminAuthService(AdminRepository adminRepository,
            SuperAdminRepository superAdminRepository,
            PasswordEncoder passwordEncoder,
            AdminTokenUtil adminTokenUtil) {
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminTokenUtil = adminTokenUtil;
    }

    public AdminAuthResponse login(AdminLoginRequest request) {
        String fullName = request.getUsername(); // The DTO field is still 'username' but holds full name now
        log.info("Attempting login for full name: '{}' (length: {})", fullName,
                fullName != null ? fullName.length() : 0);

        // 1. Try SuperAdmin (Plain-text)
        Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository
                .findByFullName(fullName);

        if (superAdminOpt.isPresent()) {
            com.iptv.wiseplayer.domain.entity.SuperAdmin superAdmin = superAdminOpt.get();
            log.info("Found SuperAdmin record for full name: '{}'", fullName);
            if (superAdmin.getPassword().equals(request.getPassword())) {
                String token = adminTokenUtil.generateToken(superAdmin.getUsername(), AdminRole.SUPER_ADMIN);
                return new AdminAuthResponse(true, token, superAdmin.getUsername(), superAdmin.getFullName(),
                        AdminRole.SUPER_ADMIN.name());
            } else {
                log.warn("Password mismatch for SuperAdmin: '{}'", fullName);
            }
        }

        // 2. Try Admin (Hashed)
        Admin admin = adminRepository.findByFullName(fullName)
                .orElseGet(() -> {
                    log.error("User not found in database with full name: '{}'", fullName);
                    throw new RuntimeException("Invalid credentials: user not found");
                });

        log.info("Found Admin record for full name: '{}', ID: {}", fullName, admin.getId());

        if (!admin.isActive()) {
            log.warn("Account is disabled for full name: '{}'", fullName);
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            log.warn("Password mismatch for Admin: '{}'", fullName);
            throw new RuntimeException("Invalid credentials: password mismatch");
        }

        log.info("Login successful for full name: '{}'", fullName);
        String token = adminTokenUtil.generateToken(admin.getUsername(), admin.getRole());

        return new AdminAuthResponse(true, token, admin.getUsername(), admin.getFullName(), admin.getRole().name());
    }
}
