package com.iptv.wiseplayer.config;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(SuperAdminRepository superRepo, AdminRepository adminRepo,
            PasswordEncoder encoder) {
        return args -> {
            // Seed SuperAdmin
            if (superRepo.count() == 0) {
                SuperAdmin superAdmin = new SuperAdmin();
                superAdmin.setUsername("superadmin");
                superAdmin.setPassword("password123");
                superAdmin.setFullName("Default Super Admin");
                superRepo.save(superAdmin);
                System.out.println("Default SuperAdmin seeded: superadmin / password123");

                // Also seed 'admin' for convenience
                SuperAdmin adminUser = new SuperAdmin();
                adminUser.setUsername("admin");
                adminUser.setPassword("admin123");
                adminUser.setFullName("Admin User");
                superRepo.save(adminUser);
                System.out.println("Admin User seeded: admin / admin123");
            }

            // Seed a Test Reseller
            if (adminRepo.findByUsername("testreseller").isEmpty()) {
                Admin reseller = new Admin();
                reseller.setUsername("testreseller");
                reseller.setPasswordHash(encoder.encode("password123"));
                reseller.setFullName("Test Reseller");
                reseller.setRole(AdminRole.RESELLER);
                reseller.setActive(true);
                adminRepo.save(reseller);
                System.out.println("Test Reseller seeded: testreseller / password123");
            }
        };
    }
}
