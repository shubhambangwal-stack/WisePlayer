package com.iptv.wiseplayer.job;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job to ensure default admin and superadmin credentials exist.
 */
@Component
public class AdminCredentialsScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdminCredentialsScheduler.class);

    private final SuperAdminRepository superRepo;
    private final AdminRepository adminRepo;
    private final PasswordEncoder encoder;

    public AdminCredentialsScheduler(SuperAdminRepository superRepo, AdminRepository adminRepo, PasswordEncoder encoder) {
        this.superRepo = superRepo;
        this.adminRepo = adminRepo;
        this.encoder = encoder;
    }

    // Runs every week (e.g., Sunday at midnight: 0 0 0 * * SUN)
    // You can also use fixedRate if preferred, e.g., @Scheduled(fixedRate = 604800000) for exactly 7 days in milliseconds.
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void ensureDefaultAdminCredentials() {
        log.info("Running weekly check for default admin and superadmin credentials...");

        // Ensure SuperAdmin exists
        if (superRepo.findByUsername("superadmin").isEmpty()) {
            SuperAdmin superAdmin = new SuperAdmin();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(encoder.encode("password123")); // Default password
            superAdmin.setEmail("superadmin@wiseplayer.com");
            superAdmin.setFullName("Default Super Admin");
            superRepo.save(superAdmin);
            log.info("Default SuperAdmin restored/seeded: superadmin");
        }

        // Ensure regular Admin exists
        if (adminRepo.findByUsername("admin").isEmpty()) {
            Admin adminUser = new Admin();
            adminUser.setUsername("admin");
            adminUser.setPasswordHash(encoder.encode("admin123")); // Default password
            adminUser.setEmail("admin@wiseplayer.com");
            adminUser.setFullName("Admin User");
            adminUser.setRole(AdminRole.ADMIN);
            adminUser.setActive(true);
            adminRepo.save(adminUser);
            log.info("Default Admin restored/seeded: admin");
        }
    }
}
