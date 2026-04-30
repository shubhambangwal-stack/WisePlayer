package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.request.AdminLoginRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.exception.AuthenticationException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import java.util.Optional;
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
        String loginUsername = request.getUsername();
        log.info("Attempting login for username: '{}'", loginUsername);

        // 1. Try SuperAdmin
        Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository
                .findByUsername(loginUsername);

        if (superAdminOpt.isPresent()) {
            com.iptv.wiseplayer.domain.entity.SuperAdmin superAdmin = superAdminOpt.get();
            log.info("Found SuperAdmin record for username: '{}'", loginUsername);
            if (passwordEncoder.matches(request.getPassword(), superAdmin.getPassword())) {
                String token = adminTokenUtil.generateToken(superAdmin.getUsername(), AdminRole.SUPER_ADMIN);
                return new AdminAuthResponse(true, token, null, superAdmin.getUsername(), superAdmin.getFullName(),
                        AdminRole.SUPER_ADMIN.name());
            } else {
                log.warn("Password mismatch for SuperAdmin: '{}'", loginUsername);
            }
        }

        // 2. Try Admin (Find by username)
        Admin admin = adminRepository.findByUsername(loginUsername)
                .orElseGet(() -> {
                    log.error("User not found in database with username: '{}'", loginUsername);
                    throw new AuthenticationException("Invalid credentials: user not found");
                });

        log.info("Found Admin record for username: '{}', ID: {}", loginUsername, admin.getId());

        if (!admin.isActive()) {
            log.warn("Account is disabled for username: '{}'", loginUsername);
            throw new AuthenticationException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            log.warn("Password mismatch for Admin: '{}'", loginUsername);
            throw new AuthenticationException("Invalid credentials: password mismatch");
        }

        log.info("Login successful for username: '{}'", loginUsername);
        String token = adminTokenUtil.generateToken(admin.getUsername(), admin.getRole());

        return new AdminAuthResponse(true, token, admin.getEmail(), admin.getUsername(), admin.getFullName(),
                admin.getRole().name());
    }
}
