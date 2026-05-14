package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.request.AdminLoginRequest;
import com.iptv.wiseplayer.dto.request.ResetPasswordRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.exception.AuthenticationException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import com.iptv.wiseplayer.domain.entity.PasswordResetToken;
import com.iptv.wiseplayer.repository.PasswordResetTokenRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminTokenUtil adminTokenUtil;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.security.admin-frontend-url}")
    private String adminFrontendUrl;

    public AdminAuthService(AdminRepository adminRepository,
            SuperAdminRepository superAdminRepository,
            PasswordEncoder passwordEncoder,
            AdminTokenUtil adminTokenUtil,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService) {
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminTokenUtil = adminTokenUtil;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
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

    public void changePassword(String username, com.iptv.wiseplayer.dto.request.ChangePasswordRequest request) {
        log.info("Attempting to change password for username: '{}'", username);

        // 1. Check SuperAdmin
        Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository.findByUsername(username);
        if (superAdminOpt.isPresent()) {
            com.iptv.wiseplayer.domain.entity.SuperAdmin superAdmin = superAdminOpt.get();
            if (!passwordEncoder.matches(request.getCurrentPassword(), superAdmin.getPassword())) {
                log.warn("Password change failed for SuperAdmin '{}': incorrect current password", username);
                throw new AuthenticationException("Current password is incorrect");
            }
            superAdmin.setPassword(passwordEncoder.encode(request.getNewPassword()));
            superAdminRepository.save(superAdmin);
            log.info("Password changed successfully for SuperAdmin: '{}'", username);
            return;
        }

        // 2. Check Admin
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
                log.warn("Password change failed for Admin '{}': incorrect current password", username);
                throw new AuthenticationException("Current password is incorrect");
            }
            admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            adminRepository.save(admin);
            log.info("Password changed successfully for Admin: '{}'", username);
            return;
        }

        log.error("Password change failed: user not found with username: '{}'", username);
        throw new com.iptv.wiseplayer.exception.ResourceNotFoundException("User not found");
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Initiating password reset for email: '{}'", email);

        // 1. Verify user exists (either Admin or SuperAdmin)
        boolean exists = adminRepository.existsByEmail(email) || superAdminRepository.existsByEmail(email);
        
        if (!exists) {
            log.warn("Password reset requested for non-existent email: '{}'", email);
            throw new com.iptv.wiseplayer.exception.ResourceNotFoundException("User not found with this email");
        }

        // 2. Generate and save token
        tokenRepository.deleteByEmail(email);
        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                token, email, java.time.LocalDateTime.now().plusHours(1));
        tokenRepository.save(resetToken);

        // 3. Send email
        String resetLink = adminFrontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(email, resetLink);
        log.info("Password reset email sent to: '{}'", email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Attempting to reset password with token");

        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new AuthenticationException("Invalid or expired token"));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new AuthenticationException("Token has expired");
        }

        String email = token.getEmail();
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // Update SuperAdmin if exists
        Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository.findByEmail(email);
        if (superAdminOpt.isPresent()) {
            com.iptv.wiseplayer.domain.entity.SuperAdmin superAdmin = superAdminOpt.get();
            superAdmin.setPassword(encodedPassword);
            superAdminRepository.save(superAdmin);
            tokenRepository.delete(token);
            log.info("Password reset successful for SuperAdmin with email: '{}'", email);
            return;
        }

        // Update Admin if exists
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            admin.setPasswordHash(encodedPassword);
            adminRepository.save(admin);
            tokenRepository.delete(token);
            log.info("Password reset successful for Admin with email: '{}'", email);
            return;
        }

        throw new com.iptv.wiseplayer.exception.ResourceNotFoundException("User not found for the provided token");
    }
}
