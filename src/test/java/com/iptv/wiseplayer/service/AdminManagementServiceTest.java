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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminManagementServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AdminInviteRepository adminInviteRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdminManagementService adminManagementService;

    private final UUID inviterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void inviteAdmin_ShouldCreateInvite_WhenInviterIsSuperAdmin() {
        // Arrange
        String email = "newadmin@test.com";
        when(adminRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(adminInviteRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        var result = adminManagementService.inviteAdmin(email, inviterId, true, request);

        // Assert
        assertTrue(result.isNew());
        assertNotNull(result.token());
        verify(adminInviteRepository).save(any(AdminInvite.class));
        verify(emailService).sendAdminInvitation(eq(email), anyString());
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void inviteAdmin_ShouldThrowException_WhenInviterIsNotSuperAdmin() {
        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> 
            adminManagementService.inviteAdmin("test@test.com", inviterId, false, request));
    }

    @Test
    void inviteAdmin_ShouldThrowException_WhenAdminAlreadyExists() {
        // Arrange
        String email = "exists@test.com";
        when(adminRepository.findByEmail(email)).thenReturn(Optional.of(new Admin()));

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> 
            adminManagementService.inviteAdmin(email, inviterId, true, request));
    }

    @Test
    void completeSetup_ShouldCreateAdminAndMarkInviteUsed_WhenTokenIsValid() {
        // Arrange
        String token = "valid-token";
        AdminInvite invite = new AdminInvite();
        invite.setToken(token);
        invite.setEmail("new@test.com");
        invite.setExpiresAt(LocalDateTime.now().plusHours(1));
        invite.setInvitedBy(inviterId);
        invite.setUsed(false);

        when(adminInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(passwordEncoder.encode("password")).thenReturn("hashed_password");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        adminManagementService.completeSetup(token, "password", "New Admin", request);

        // Assert
        verify(adminRepository).save(argThat(admin -> 
            admin.getEmail().equals("new@test.com") && 
            admin.getRole() == AdminRole.ADMIN &&
            admin.isActive()
        ));
        assertTrue(invite.isUsed());
        verify(adminInviteRepository).save(invite);
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void completeSetup_ShouldThrowException_WhenInviteExpired() {
        // Arrange
        String token = "expired-token";
        AdminInvite invite = new AdminInvite();
        invite.setToken(token);
        invite.setExpiresAt(LocalDateTime.now().minusHours(1));
        invite.setUsed(false);

        when(adminInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        // Act & Assert
        assertThrows(InvalidInvitationException.class, () -> 
            adminManagementService.completeSetup(token, "pass", "Name", request));
    }
}
