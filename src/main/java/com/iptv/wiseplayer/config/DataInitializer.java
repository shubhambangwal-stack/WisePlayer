package com.iptv.wiseplayer.config;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import com.iptv.wiseplayer.service.RolePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to seed initial admin data on-demand via a protected internal endpoint.
 * This does NOT run on startup. It must be triggered manually.
 */
@Service
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SuperAdminRepository superRepo;
    private final AdminRepository adminRepo;
    private final PasswordEncoder encoder;
    private final RolePermissionService rolePermissionService;

    public DataInitializer(SuperAdminRepository superRepo, AdminRepository adminRepo,
                           PasswordEncoder encoder, RolePermissionService rolePermissionService) {
        this.superRepo = superRepo;
        this.adminRepo = adminRepo;
        this.encoder = encoder;
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * Seeds default SuperAdmin, Admin, and Test Reseller accounts if they don't already exist.
     *
     * @return List of messages describing what was seeded.
     */
    @Transactional
    public List<String> seedData() {
        List<String> results = new ArrayList<>();

        // Seed role_permissions defaults (idempotent – skips existing rows)
        rolePermissionService.seedDefaults();
        results.add("Role permission defaults seeded.");

        // Seed SuperAdmin
        if (superRepo.count() == 0) {
            SuperAdmin superAdmin = new SuperAdmin();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(encoder.encode("password123"));
            superAdmin.setEmail("superadmin@wiseplayer.com");
            superAdmin.setFullName("Default Super Admin");
            superRepo.save(superAdmin);
            log.info("Default SuperAdmin seeded: superadmin");
            results.add("SuperAdmin seeded: superadmin");

            // Seed a regular Admin
            if (adminRepo.findByUsername("admin").isEmpty()) {
                Admin adminUser = new Admin();
                adminUser.setUsername("admin");
                adminUser.setPasswordHash(encoder.encode("admin123"));
                adminUser.setEmail("admin@wiseplayer.com");
                adminUser.setFullName("Admin User");
                adminUser.setRole(AdminRole.ADMIN);
                adminUser.setActive(true);
                adminRepo.save(adminUser);
                log.info("Regular Admin seeded: admin");
                results.add("Admin seeded: admin");
            }
        } else {
            results.add("SuperAdmin already exists, skipping.");
        }

        // Seed a Test Reseller
        if (adminRepo.findByEmail("reseller@test.com").isEmpty()) {
            Admin reseller = new Admin();
            reseller.setEmail("reseller@test.com");
            reseller.setUsername("Test Reseller");
            reseller.setPasswordHash(encoder.encode("password123"));
            reseller.setRole(AdminRole.RESELLER);
            reseller.setActive(true);
            adminRepo.save(reseller);
            log.info("Test Reseller seeded: reseller@test.com");
            results.add("Test Reseller seeded: reseller@test.com");
        } else {
            results.add("Test Reseller already exists, skipping.");
        }

        return results;
    }
}
