package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.AdminAuditLog;
import com.iptv.wiseplayer.domain.entity.AdminInvite;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.exception.InvalidInvitationException;
import com.iptv.wiseplayer.exception.ResourceAlreadyExistsException;
import com.iptv.wiseplayer.repository.AdminAuditLogRepository;
import com.iptv.wiseplayer.repository.AdminInviteRepository;
import com.iptv.wiseplayer.repository.AdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AdminManagementService {

    private final AdminRepository adminRepository;
    private final AdminInviteRepository adminInviteRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public AdminManagementService(AdminRepository adminRepository,
            AdminInviteRepository adminInviteRepository,
            AdminAuditLogRepository adminAuditLogRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.adminRepository = adminRepository;
        this.adminInviteRepository = adminInviteRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public record InviteResult(String token, boolean isNew, long minutesRemaining) {
    }

    @Transactional
    public InviteResult inviteAdmin(String email, UUID inviterId, boolean isSuperAdmin, HttpServletRequest request) {
        if (!isSuperAdmin) {
            throw new AccessDeniedException("Only SUPER_ADMIN can invite new admins");
        }

        if (adminRepository.findByUsername(email).isPresent()) {
            throw new ResourceAlreadyExistsException("Admin with this email already exists");
        }

        Optional<AdminInvite> existingInvite = adminInviteRepository.findByEmail(email);
        if (existingInvite.isPresent() && !existingInvite.get().isUsed()
                && existingInvite.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            AdminInvite invite = existingInvite.get();
            log.info("Found existing valid invitation for {}, re-sending email. Previous expiry: {}", email,
                    invite.getExpiresAt());
            invite.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            adminInviteRepository.save(invite);

            String inviteLink = baseUrl + "/admin/setup?token=" + invite.getToken();
            log.info("Re-sending invite link: {}", inviteLink);
            emailService.sendAdminInvitation(email, inviteLink);

            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), invite.getExpiresAt()).toMinutes();
            return new InviteResult(invite.getToken(), false, Math.max(0, minutesRemaining));
        }

        String token = UUID.randomUUID().toString();
        AdminInvite invite = new AdminInvite();
        invite.setEmail(email);
        invite.setToken(token);
        invite.setInvitedBy(inviterId);
        invite.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        adminInviteRepository.save(invite);

        // Send Email
        String inviteLink = baseUrl + "/admin/setup?token=" + token;
        log.info("Sending NEW invite link: {}", inviteLink);
        emailService.sendAdminInvitation(email, inviteLink);

        // Logging
        AdminAuditLog auditLog = new AdminAuditLog(inviterId, email, "INVITE_SENT", request.getRemoteAddr());
        adminAuditLogRepository.save(auditLog);

        return new InviteResult(token, true, 5);
    }

    public AdminInvite verifyInvite(String token) {
        AdminInvite invite = adminInviteRepository.findByToken(token)
                .orElseThrow(() -> new InvalidInvitationException("Invalid invitation token"));

        if (invite.isUsed()) {
            throw new InvalidInvitationException("Invitation has already been used");
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInvitationException("Invitation has expired");
        }

        return invite;
    }

    @Transactional
    public void completeSetup(String token, String password, String fullName, HttpServletRequest request) {
        AdminInvite invite = verifyInvite(token);

        Admin admin = new Admin();
        admin.setUsername(invite.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName(fullName);
        admin.setRole(AdminRole.ADMIN);
        admin.setActive(true);
        adminRepository.save(admin);

        invite.setUsed(true);
        adminInviteRepository.save(invite);

        // Logging
        AdminAuditLog auditLog = new AdminAuditLog(invite.getInvitedBy(), invite.getEmail(), "ADMIN_CREATED",
                request.getRemoteAddr());
        adminAuditLogRepository.save(auditLog);
    }
}
