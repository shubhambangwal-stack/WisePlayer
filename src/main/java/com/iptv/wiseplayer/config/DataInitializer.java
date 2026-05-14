package com.iptv.wiseplayer.config;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@org.springframework.context.annotation.Profile("!prod")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(SuperAdminRepository superRepo, AdminRepository adminRepo,
            PasswordEncoder encoder) {
        return args -> {
            // Seed SuperAdmin
            if (superRepo.count() == 0) {
                SuperAdmin superAdmin = new SuperAdmin();
                superAdmin.setUsername("superadmin");
                superAdmin.setPassword(encoder.encode("password123"));
                superAdmin.setEmail("superadmin@wiseplayer.com");
                superAdmin.setFullName("Default Super Admin");
                superRepo.save(superAdmin);
                log.info("Default SuperAdmin seeded: superadmin / password123 (hashed)");

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
                    log.info("Regular Admin seeded: admin / admin123 (hashed)");
                }
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
                log.info("Test Reseller seeded: reseller@test.com / password123");
            }
        };
    }
}
