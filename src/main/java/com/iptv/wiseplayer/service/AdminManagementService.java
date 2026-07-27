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
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminManagementService {

    private static final Logger log = LoggerFactory.getLogger(AdminManagementService.class);

    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final AdminInviteRepository adminInviteRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public AdminManagementService(AdminRepository adminRepository,
            SuperAdminRepository superAdminRepository,
            AdminInviteRepository adminInviteRepository,
            AdminAuditLogRepository adminAuditLogRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
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

        if (adminRepository.findByEmail(email).isPresent() || superAdminRepository.findByEmail(email).isPresent()) {
            throw new ResourceAlreadyExistsException("User with this email already exists (Admin or Super Admin)");
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
        admin.setEmail(invite.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setUsername(fullName);
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

    public com.iptv.wiseplayer.dto.response.AdminResponse getAdminById(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new com.iptv.wiseplayer.exception.ResourceNotFoundException("Admin not found with ID: " + id));
        return mapToAdminResponse(admin);
    }

    @Transactional
    public com.iptv.wiseplayer.dto.response.AdminResponse createAdminDirect(com.iptv.wiseplayer.dto.request.CreateAdminRequest request) {
        if (request.getEmail() != null && (adminRepository.findByEmail(request.getEmail()).isPresent() || superAdminRepository.findByEmail(request.getEmail()).isPresent())) {
            throw new ResourceAlreadyExistsException("User with this email already exists");
        }
        if (adminRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResourceAlreadyExistsException("Admin with this username already exists");
        }

        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setEmail(request.getEmail());
        admin.setFullName(request.getFullName());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (request.getRole() != null) {
            try {
                admin.setRole(AdminRole.valueOf(request.getRole().toUpperCase()));
            } catch (Exception e) {
                admin.setRole(AdminRole.ADMIN);
            }
        } else {
            admin.setRole(AdminRole.ADMIN);
        }
        admin.setActive(true);

        adminRepository.save(admin);
        return mapToAdminResponse(admin);
    }

    @Transactional
    public com.iptv.wiseplayer.dto.response.AdminResponse updateAdmin(UUID id, com.iptv.wiseplayer.dto.request.UpdateAdminRequest request) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new com.iptv.wiseplayer.exception.ResourceNotFoundException("Admin not found with ID: " + id));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            admin.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            admin.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            admin.setFullName(request.getFullName());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getActive() != null) {
            admin.setActive(request.getActive());
        }
        if (request.getCredits() != null) {
            admin.setCredits(request.getCredits());
        }
        if (request.getRole() != null) {
            admin.setRole(request.getRole());
        }
        if (request.getCanCreate() != null) {
            admin.setCanCreate(request.getCanCreate());
        }
        if (request.getCanRead() != null) {
            admin.setCanRead(request.getCanRead());
        }
        if (request.getCanUpdate() != null) {
            admin.setCanUpdate(request.getCanUpdate());
        }
        if (request.getCanDelete() != null) {
            admin.setCanDelete(request.getCanDelete());
        }

        adminRepository.save(admin);
        return mapToAdminResponse(admin);
    }

    @Transactional
    public com.iptv.wiseplayer.dto.response.AdminResponse toggleAdminStatus(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new com.iptv.wiseplayer.exception.ResourceNotFoundException("Admin not found with ID: " + id));
        admin.setActive(!admin.isActive());
        adminRepository.save(admin);
        return mapToAdminResponse(admin);
    }

    @Transactional
    public void deleteAdmin(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new com.iptv.wiseplayer.exception.ResourceNotFoundException("Admin not found with ID: " + id));
        adminRepository.delete(admin);
    }

    public java.util.List<com.iptv.wiseplayer.dto.response.AdminResponse> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(this::mapToAdminResponse)
                .toList();
    }

    private com.iptv.wiseplayer.dto.response.AdminResponse mapToAdminResponse(Admin admin) {
        return new com.iptv.wiseplayer.dto.response.AdminResponse(
                admin.getId(),
                admin.getEmail(),
                admin.getUsername(),
                admin.getFullName(),
                admin.getRole().name(),
                admin.isActive(),
                admin.getCreatedAt(),
                admin.getCredits(),
                admin.isCanCreate(),
                admin.isCanRead(),
                admin.isCanUpdate(),
                admin.isCanDelete()
        );
    }
}

