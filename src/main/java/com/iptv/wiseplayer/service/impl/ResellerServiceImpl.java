package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.*;

import java.math.BigDecimal;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.dto.request.*;
import com.iptv.wiseplayer.dto.response.ActivationRequestResponse;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.AuthenticationException;
import com.iptv.wiseplayer.exception.ResourceAlreadyExistsException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.repository.*;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import com.iptv.wiseplayer.security.CrudOperation;
import com.iptv.wiseplayer.security.CrudPermissionGuard;
import com.iptv.wiseplayer.security.DeviceTokenUtil;
import com.iptv.wiseplayer.security.RequiresCrud;
import com.iptv.wiseplayer.service.EmailService;
import com.iptv.wiseplayer.service.ResellerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResellerServiceImpl implements ResellerService {

    private static final Logger log = LoggerFactory.getLogger(ResellerServiceImpl.class);

    private final DeviceRepository deviceRepository;
    private final AdminRepository adminRepository;
    private final ActivationRequestRepository activationRequestRepository;
    private final DeviceTokenUtil tokenUtil;
    private final AdminTokenUtil adminTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final com.iptv.wiseplayer.service.CreditService creditService;
    private final com.iptv.wiseplayer.repository.SubscriptionRepository subscriptionRepository;
    private final com.iptv.wiseplayer.repository.ResellerCustomerRepository resellerCustomerRepository;

    private final ResellerEmailOtpRepository resellerEmailOtpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final com.iptv.wiseplayer.repository.RolePermissionRepository rolePermissionRepository;
    private final CrudPermissionGuard crudPermissionGuard;

    public ResellerServiceImpl(DeviceRepository deviceRepository,
                               AdminRepository adminRepository,
                               ActivationRequestRepository activationRequestRepository,
                               DeviceTokenUtil tokenUtil,
                               AdminTokenUtil adminTokenUtil,
                               PasswordEncoder passwordEncoder,
                               com.iptv.wiseplayer.service.CreditService creditService,
                               com.iptv.wiseplayer.repository.SubscriptionRepository subscriptionRepository,
                               com.iptv.wiseplayer.repository.ResellerCustomerRepository resellerCustomerRepository,
                               ResellerEmailOtpRepository resellerEmailOtpRepository,
                               PasswordResetTokenRepository passwordResetTokenRepository,
                               EmailService emailService,
                               com.iptv.wiseplayer.repository.RolePermissionRepository rolePermissionRepository,
                               CrudPermissionGuard crudPermissionGuard) {
        this.deviceRepository = deviceRepository;
        this.adminRepository = adminRepository;
        this.activationRequestRepository = activationRequestRepository;
        this.tokenUtil = tokenUtil;
        this.adminTokenUtil = adminTokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.creditService = creditService;
        this.subscriptionRepository = subscriptionRepository;
        this.resellerCustomerRepository = resellerCustomerRepository;
        this.resellerEmailOtpRepository = resellerEmailOtpRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.rolePermissionRepository = rolePermissionRepository;
        this.crudPermissionGuard = crudPermissionGuard;
    }

    @Override
    @Transactional
    public AdminAuthResponse login(ResellerLoginRequest request) {
        Admin admin = null;
        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            admin = adminRepository.findByUsername(request.getUsername()).orElse(null);
            if (admin == null) {
                admin = adminRepository.findByEmail(request.getUsername()).orElse(null);
            }
        }
        if (admin == null) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new AccessDeniedException("Access denied: Not a reseller account");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        String token = adminTokenUtil.generateToken(admin.getUsername(), admin.getRole(),
                admin.isCanCreate(), admin.isCanRead(), admin.isCanUpdate(), admin.isCanDelete());

        if (!admin.isActive()) {
            // Check if a valid (non-expired) OTP already exists
            boolean hasValidOtp = resellerEmailOtpRepository.findByAdminId(admin.getId())
                    .map(otp -> LocalDateTime.now().isBefore(otp.getExpiresAt()))
                    .orElse(false);

            if (!hasValidOtp) {
                try {
                    sendOtpForUser(admin);
                } catch (Exception e) {
                    log.error("Failed to send OTP for user: {}", admin.getUsername(), e);
                    throw new BadRequestException("Failed to send OTP. Please try again.");
                }
            }

            return new AdminAuthResponse(false, token, admin.getEmail(), admin.getUsername(),
                    admin.getFullName(), admin.getRole().name(),
                    admin.isCanCreate(), admin.isCanRead(), admin.isCanUpdate(), admin.isCanDelete());
        }

        return new AdminAuthResponse(true, token, admin.getEmail(), admin.getUsername(),
                admin.getFullName(), admin.getRole().name(),
                admin.isCanCreate(), admin.isCanRead(), admin.isCanUpdate(), admin.isCanDelete());
    }

    @Override
    @Transactional
    public AdminAuthResponse register(ResellerRegisterRequest request) {
        Admin reseller;

        java.util.Optional<Admin> byEmail = adminRepository.findByEmail(request.getEmail());
        java.util.Optional<Admin> byUsername = adminRepository.findByUsername(request.getUsername());

        if (byEmail.isPresent()) {
            Admin existing = byEmail.get();
            if (existing.isActive()) {
                throw new ResourceAlreadyExistsException("Email already registered");
            }
            // Unverified account with this email exists — reuse it, update all fields including username
            existing.setUsername(request.getUsername());
            existing.setFullName(request.getFullName());
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            reseller = adminRepository.save(existing);
        } else if (byUsername.isPresent()) {
            Admin existing = byUsername.get();
            if (existing.isActive()) {
                throw new ResourceAlreadyExistsException("Username already exists");
            }
            // Unverified account with this username exists — reuse it, update email too
            existing.setEmail(request.getEmail());
            existing.setFullName(request.getFullName());
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            reseller = adminRepository.save(existing);
        } else {
            // Apply role-level defaults from role_permissions table before saving
            com.iptv.wiseplayer.domain.entity.RolePermission defaults =
                    rolePermissionRepository.findByRole(AdminRole.RESELLER)
                            .orElse(com.iptv.wiseplayer.domain.entity.RolePermission.allTrue(AdminRole.RESELLER));

            reseller = new Admin();
            reseller.setUsername(request.getUsername());
            reseller.setFullName(request.getFullName());
            reseller.setEmail(request.getEmail());
            reseller.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            reseller.setRole(AdminRole.RESELLER);
            reseller.setActive(false);
            // Apply role defaults instead of hardcoded true
            reseller.setCanCreate(defaults.isCanCreate());
            reseller.setCanRead(defaults.isCanRead());
            reseller.setCanUpdate(defaults.isCanUpdate());
            reseller.setCanDelete(defaults.isCanDelete());
            reseller = adminRepository.save(reseller);
        }

        Admin saved = reseller;

        emailService.sendWelcomeEmail(saved.getEmail(), saved.getUsername(), saved.getFullName());

        String token = adminTokenUtil.generateToken(saved.getUsername(), saved.getRole(), saved.isCanCreate(), saved.isCanRead(), saved.isCanUpdate(), saved.isCanDelete());
        return new AdminAuthResponse(true, token, saved.getEmail(), saved.getUsername(), saved.getFullName(),
                saved.getRole().name(), saved.isCanCreate(), saved.isCanRead(), saved.isCanUpdate(), saved.isCanDelete());
    }
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredOtps() {
        resellerEmailOtpRepository.deleteAllExpired(LocalDateTime.now());
        log.info("Purged expired OTPs from reseller_email_otps");
    }
    @Override
    public ResellerDashboardResponse getDashboardOverview(UUID resellerId) {
        ResellerDashboardResponse response = new ResellerDashboardResponse();
        response.setTotalUsers(deviceRepository.countByResellerId(resellerId));
        response.setActiveSubscriptions(
                deviceRepository.countByResellerIdAndDeviceStatus(resellerId, DeviceStatus.ACTIVE));
        response.setPendingRequests(activationRequestRepository.countByResellerIdAndStatus(resellerId, "PENDING"));
        response.setCredits(creditService.getBalance(resellerId));
        response.setRecentUsers(deviceRepository.findTop5ByResellerIdOrderByCreatedAtDesc(resellerId));
        return response;
    }

    @Override
    @Transactional
    @RequiresCrud(CrudOperation.CREATE)
    public java.util.Map<String, Object> createEndUser(UUID resellerId, DeviceRegistrationRequest request) {
        String macAddress = request.getDeviceId();
        
        // Check if the device exists in the devices table (registered through the app)
        String fingerprintHash = tokenUtil.hashFingerprint(macAddress);
        java.util.Optional<Device> existingDevice = deviceRepository.findByFingerprintHash(fingerprintHash);
        
        if (existingDevice.isEmpty()) {
            throw new ResourceNotFoundException("Device not found. Only devices registered through the app can be added.");
        }
        
        // Check if device already claimed by this reseller
        if (resellerCustomerRepository.findByResellerIdAndMacAddress(resellerId, macAddress).isPresent()) {
            throw new ResourceAlreadyExistsException("You have already added this device.");
        }
        
        // Check if device is claimed by someone else
        if (resellerCustomerRepository.findByMacAddress(macAddress).isPresent()) {
            throw new ResourceAlreadyExistsException("This device is already claimed by another reseller.");
        }

        // 1. Add to reseller_customers table
        com.iptv.wiseplayer.domain.entity.ResellerCustomer rc = new com.iptv.wiseplayer.domain.entity.ResellerCustomer(resellerId, macAddress, request.getDeviceModel());
        resellerCustomerRepository.save(rc);

        // 2. Update its resellerId
        Device device = existingDevice.get();
        device.setResellerId(resellerId);
        deviceRepository.save(device);

        return java.util.Map.of(
            "success", true,
            "message", "Device successfully added to your list.",
            "macAddress", macAddress
        );
    }

    // ADD:
    @Override
    public org.springframework.data.domain.Page<Device> getResellerUsers(
            UUID resellerId,
            String search,
            DeviceStatus status,
            String subscription,
            java.time.LocalDate registeredFrom,
            java.time.LocalDate registeredTo,
            java.time.LocalDate expiresFrom,
            java.time.LocalDate expiresTo,
            org.springframework.data.domain.Pageable pageable) {

        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String subParam = (subscription != null && !subscription.trim().isEmpty()) ? subscription.trim().toUpperCase() : null;

        java.time.LocalDateTime regFrom = (registeredFrom != null) ? registeredFrom.atStartOfDay() : null;
        java.time.LocalDateTime regTo   = (registeredTo   != null) ? registeredTo.atTime(23, 59, 59) : null;
        java.time.LocalDateTime expFrom = (expiresFrom    != null) ? expiresFrom.atStartOfDay() : null;
        java.time.LocalDateTime expTo   = (expiresTo      != null) ? expiresTo.atTime(23, 59, 59) : null;

        return deviceRepository.searchResellerUsers(
                resellerId, searchParam, status, subParam,
                regFrom, regTo, expFrom, expTo,
                pageable);
    }

    @Override
    @Transactional
    @RequiresCrud(CrudOperation.UPDATE)
    public void disableUser(UUID resellerId, UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        if (!resellerId.equals(device.getResellerId())) {
            throw new AccessDeniedException("Permission denied");
        }

        boolean newStatus = !device.isActive();
        device.setActive(newStatus);
        device.setDeviceStatus(newStatus ? DeviceStatus.ACTIVE : DeviceStatus.INACTIVE);

        deviceRepository.save(device);
    }

    @Override
    @Transactional
    @RequiresCrud(CrudOperation.CREATE)
    public Admin createSubReseller(UUID resellerId, SubResellerCreateRequest request) {
        if (adminRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Admin currentAdmin = adminRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));

        if (!currentAdmin.getRole().canManage(AdminRole.SUB_RESELLER)) {
            throw new AccessDeniedException("You cannot create a sub-reseller.");
        }

        // Centralized escalation check — cannot grant flags you don't possess
        crudPermissionGuard.checkEscalation(currentAdmin,
                request.getCanCreate(), request.getCanRead(),
                request.getCanUpdate(), request.getCanDelete());

        // Apply SUB_RESELLER role defaults from role_permissions table
        com.iptv.wiseplayer.domain.entity.RolePermission subDefaults =
                rolePermissionRepository.findByRole(AdminRole.SUB_RESELLER)
                        .orElse(com.iptv.wiseplayer.domain.entity.RolePermission.allTrue(AdminRole.SUB_RESELLER));

        Admin sub = new Admin();
        sub.setUsername(request.getUsername());
        sub.setFullName(request.getFullName());
        sub.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        sub.setRole(AdminRole.SUB_RESELLER);
        sub.setParentId(resellerId);
        sub.setCreatorId(resellerId);
        // Start from role defaults, then overlay any explicit overrides from the request
        sub.setCanCreate(subDefaults.isCanCreate());
        sub.setCanRead(subDefaults.isCanRead());
        sub.setCanUpdate(subDefaults.isCanUpdate());
        sub.setCanDelete(subDefaults.isCanDelete());

        if (request.getCanCreate() != null) sub.setCanCreate(request.getCanCreate());
        if (request.getCanRead() != null) sub.setCanRead(request.getCanRead());
        if (request.getCanUpdate() != null) sub.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) sub.setCanDelete(request.getCanDelete());

        // 100% Accurate Escalation Prevention: Cap permissions strictly to the creator's permissions.
        // This prevents loopholes where role defaults might exceed the caller's own permissions.
        if (!currentAdmin.isCanCreate()) sub.setCanCreate(false);
        if (!currentAdmin.isCanRead()) sub.setCanRead(false);
        if (!currentAdmin.isCanUpdate()) sub.setCanUpdate(false);
        if (!currentAdmin.isCanDelete()) sub.setCanDelete(false);

        return adminRepository.save(sub);
    }

    @Override
    public org.springframework.data.domain.Page<Admin> getSubResellers(
            UUID resellerId, String search, Boolean status,
            java.time.LocalDate fromDate, java.time.LocalDate toDate,
            java.math.BigDecimal minCredits, java.math.BigDecimal maxCredits,
            org.springframework.data.domain.Pageable pageable) {
        String fromParam = fromDate != null ? fromDate.atStartOfDay().toString() : null;
        String toParam   = toDate   != null ? toDate.atTime(23, 59, 59).toString() : null;
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return adminRepository.searchSubResellers(
                resellerId, searchParam, status, fromParam, toParam, minCredits, maxCredits, pageable);
    }

    @Override
    @Transactional
    @RequiresCrud(CrudOperation.UPDATE)
    public void updateSubReseller(UUID resellerId, UUID subResellerId,
            com.iptv.wiseplayer.dto.request.SubResellerUpdateRequest request) {
        Admin sub = adminRepository.findById(subResellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-reseller not found"));

        if (!resellerId.equals(sub.getParentId())) {
            throw new AccessDeniedException("Permission denied: Not your sub-reseller");
        }

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Admin currentAdmin = adminRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));

        if (!currentAdmin.getRole().canManage(sub.getRole())) {
            throw new AccessDeniedException("You cannot modify an equal or higher-ranked role.");
        }

        // Centralized escalation check
        crudPermissionGuard.checkEscalation(currentAdmin,
                request.getCanCreate(), request.getCanRead(),
                request.getCanUpdate(), request.getCanDelete());

        sub.setFullName(request.getFullName());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            sub.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getCanCreate() != null) sub.setCanCreate(request.getCanCreate());
        if (request.getCanRead() != null) sub.setCanRead(request.getCanRead());
        if (request.getCanUpdate() != null) sub.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) sub.setCanDelete(request.getCanDelete());

        if (!currentAdmin.isCanCreate()) sub.setCanCreate(false);
        if (!currentAdmin.isCanRead()) sub.setCanRead(false);
        if (!currentAdmin.isCanUpdate()) sub.setCanUpdate(false);
        if (!currentAdmin.isCanDelete()) sub.setCanDelete(false);

        adminRepository.save(sub);
    }

    @Override
    @Transactional
    public void updateSubResellerPermissionsById(UUID resellerId, UUID subResellerId,
            com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest request) {
        Admin sub = adminRepository.findById(subResellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-reseller not found"));

        if (!resellerId.equals(sub.getParentId())) {
            throw new AccessDeniedException("Permission denied: Not your sub-reseller");
        }

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Admin currentAdmin = adminRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));

        if (!currentAdmin.getRole().canManage(sub.getRole())) {
            throw new AccessDeniedException("You cannot modify an equal or higher-ranked role.");
        }

        // Centralized escalation check
        crudPermissionGuard.checkEscalation(currentAdmin, request);

        if (request.getCanCreate() != null) sub.setCanCreate(request.getCanCreate());
        if (request.getCanRead()   != null) sub.setCanRead(request.getCanRead());
        if (request.getCanUpdate() != null) sub.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) sub.setCanDelete(request.getCanDelete());

        if (!currentAdmin.isCanCreate()) sub.setCanCreate(false);
        if (!currentAdmin.isCanRead()) sub.setCanRead(false);
        if (!currentAdmin.isCanUpdate()) sub.setCanUpdate(false);
        if (!currentAdmin.isCanDelete()) sub.setCanDelete(false);

        adminRepository.save(sub);
    }

    @Override
    @Transactional
    @RequiresCrud(CrudOperation.UPDATE)
    public void toggleSubResellerStatus(UUID resellerId, UUID subResellerId) {
        Admin sub = adminRepository.findById(subResellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-reseller not found"));

        if (!resellerId.equals(sub.getParentId())) {
            throw new AccessDeniedException("Permission denied: Not your sub-reseller");
        }

        sub.setActive(!sub.isActive());
        adminRepository.save(sub);
    }

    @Override
    @Transactional
    @RequiresCrud(CrudOperation.DELETE)
    public void deleteSubReseller(UUID resellerId, UUID subResellerId) {
        Admin sub = adminRepository.findById(subResellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-reseller not found"));

        if (!resellerId.equals(sub.getParentId())) {
            throw new AccessDeniedException("Permission denied: Not your sub-reseller");
        }

        if (sub.getRole() != AdminRole.SUB_RESELLER) {
            throw new AccessDeniedException("Can only delete sub-resellers");
        }

        if (sub.getCredits() != null) {
            throw new AccessDeniedException("Can't delete subreseller with credits");
        }
        adminRepository.delete(sub);
    }
    @Override
    @Transactional
    public ActivationRequest submitActivationRequest(UUID resellerId, ResellerActivationRequestDto requestDto) {
        UUID deviceId = requestDto.getDeviceId();
        String planName = requestDto.getPlanName();
        String status = requestDto.getStatus();

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        if (!resellerId.equals(device.getResellerId())) {
            throw new AccessDeniedException("Permission denied");
        }

        // Check if device already has an active subscription with the same plan
        subscriptionRepository
                .findByDeviceIdAndStatus(deviceId, com.iptv.wiseplayer.domain.enums.SubscriptionStatus.ACTIVE)
                .ifPresent(sub -> {
                    if (planName.equalsIgnoreCase(sub.getPlanName())) {
                        throw new BadRequestException(
                                "Device already has an active subscription with the " + planName + " plan");
                    }
                });

        // Determine target status: use parameter if provided, otherwise default to
        // PENDING
        String targetStatus = (status != null && !status.isEmpty()) ? status.toUpperCase() : "PENDING";

        java.util.Optional<ActivationRequest> existingOpt = activationRequestRepository
                .findTopByDeviceIdOrderByCreatedAtDesc(deviceId);
        if (existingOpt.isPresent()) {
            ActivationRequest existing = existingOpt.get();

            // If the status and plan are already the same, block it
            if (targetStatus.equals(existing.getStatus()) && planName.equalsIgnoreCase(existing.getPlanName())) {
                throw new BadRequestException(
                        "An activation request for this device with status [" + targetStatus + "] already exists");
            }

            // Also block if it's already PENDING and we're trying to submit another PENDING
            if ("PENDING".equals(existing.getStatus()) && "PENDING".equals(targetStatus)) {
                throw new BadRequestException("An activation request for this device is already pending");
            }

            // Otherwise, update the existing record
            BigDecimal cost = creditService.getActivationCost(planName);
            existing.setPlanName(planName);
            existing.setAmount(cost.doubleValue());
            existing.setCurrency("CREDITS");
            existing.setStatus(targetStatus);
            existing.setResellerId(resellerId);
            return activationRequestRepository.save(existing);
        }

        BigDecimal cost = creditService.getActivationCost(planName);
        ActivationRequest request = new ActivationRequest();
        request.setResellerId(resellerId);
        request.setResellerId(resellerId);
        request.setDeviceId(deviceId);
        request.setPlanName(planName);
        request.setAmount(cost.doubleValue());
        request.setCurrency("CREDITS");
        request.setStatus(targetStatus);

        ActivationRequest saved = activationRequestRepository.save(request);

        // Deduct credits
        try {
            creditService.deductCredits(resellerId, planName, saved.getId());
            saved.setCreditsUsed(cost);
            return activationRequestRepository.save(saved);
        } catch (Exception e) {
            // If credit deduction fails, we should probably rollback or handle it
            // Transactional will handle it if we throw an exception
            log.error("Failed to deduct credits for request: {}", saved.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteActivationRequest(UUID resellerId, UUID requestId) {
        ActivationRequest activationRequest = activationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activation request not found"));

        if (!resellerId.equals(activationRequest.getResellerId())) {
            throw new AccessDeniedException("Permission denied: Not your activation request");
        }

        if ("APPROVED".equals(activationRequest.getStatus())) {
            throw new BadRequestException("Cannot delete an approved activation request");
        }

        activationRequestRepository.delete(activationRequest);
    }

    @Override
    @Transactional
    @RequiresCrud(CrudOperation.DELETE)
    public void detachDevice(UUID resellerId, UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (!resellerId.equals(device.getResellerId())) {
            throw new AccessDeniedException("Permission denied");
        }

        if (device.getMacAddress() != null) {
            resellerCustomerRepository
                    .findByResellerIdAndMacAddress(resellerId, device.getMacAddress())
                    .ifPresent(resellerCustomerRepository::delete);
        }

        device.setResellerId(null);
        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void cancelSubscription(UUID resellerId, UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (!resellerId.equals(device.getResellerId())) {
            throw new AccessDeniedException("Permission denied");
        }

        Subscription sub = subscriptionRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for this device"));

        if (sub.getStatus() == com.iptv.wiseplayer.domain.enums.SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Subscription is already cancelled");
        }

        sub.setStatus(com.iptv.wiseplayer.domain.enums.SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(sub);
    }

    @Override
    @Transactional
    public void pauseResumeSubscription(UUID resellerId, UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (!resellerId.equals(device.getResellerId())) {
            throw new AccessDeniedException("Permission denied");
        }

        Subscription sub = subscriptionRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for this device"));

        if (sub.getStatus() == com.iptv.wiseplayer.domain.enums.SubscriptionStatus.CANCELLED ||
                sub.getStatus() == com.iptv.wiseplayer.domain.enums.SubscriptionStatus.EXPIRED) {
            throw new BadRequestException("Cannot pause/resume a cancelled or expired subscription");
        }

        if (sub.getStatus() == com.iptv.wiseplayer.domain.enums.SubscriptionStatus.PAUSED) {
            sub.setStatus(com.iptv.wiseplayer.domain.enums.SubscriptionStatus.ACTIVE);
            device.setActive(true);
            device.setDeviceStatus(DeviceStatus.ACTIVE);
        } else {
            sub.setStatus(com.iptv.wiseplayer.domain.enums.SubscriptionStatus.PAUSED);
            device.setActive(false);
            device.setDeviceStatus(DeviceStatus.INACTIVE);
        }

        subscriptionRepository.save(sub);
        deviceRepository.save(device);
    }

    @Override
    public Page<ActivationRequestResponse> getResellerRequests(
            UUID resellerId, String search, String status, String planName,
            LocalDate fromDate, LocalDate toDate,
            BigDecimal minCredits, BigDecimal maxCredits,
            Pageable pageable) {

        String fromDateTime = fromDate != null ? fromDate.atStartOfDay().toString() : null;
        String toDateTime   = toDate   != null ? toDate.atTime(23, 59, 59).toString() : null;
        String searchParam = (search   != null && !search.trim().isEmpty())   ? search.trim()   : null;
        String statusParam = (status   != null && !status.trim().isEmpty())   ? status.trim()   : null;
        String planParam   = (planName != null && !planName.trim().isEmpty()) ? planName.trim() : null;

       Page<ActivationRequest> requestsPage =
                activationRequestRepository.searchResellerRequests(
                        resellerId, statusParam, planParam,
                        fromDateTime, toDateTime, minCredits, maxCredits,
                        searchParam, pageable);

        return requestsPage.map(request -> {
            com.iptv.wiseplayer.dto.response.ActivationRequestResponse response =
                    new com.iptv.wiseplayer.dto.response.ActivationRequestResponse();
            response.setId(request.getId());
            response.setResellerId(request.getResellerId());
            response.setDeviceId(request.getDeviceId());
            response.setPlanName(request.getPlanName());
            response.setAmount(request.getAmount());
            response.setCurrency(request.getCurrency());
            response.setStatus(request.getStatus());
            response.setCreditsUsed(request.getCreditsUsed());
            response.setAdminNotes(request.getAdminNotes());
            response.setReviewedBy(request.getReviewedBy());
            response.setReviewedAt(request.getReviewedAt());
            response.setCreatedAt(request.getCreatedAt());
            response.setUpdatedAt(request.getUpdatedAt());

            deviceRepository.findByDeviceId(request.getDeviceId()).ifPresent(device -> {
                response.setDeviceModel(device.getDeviceModel());
                response.setPlatform(device.getPlatform());
                response.setDeviceStatus(device.getDeviceStatus().name());
                response.setMacAddress(device.getMacAddress());
            });

            return response;
        });
    }

    @Override
    @Transactional
    public Map<String, String> verifyEmail(VerifyOtpRequest request, String username) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        ResellerEmailOtp otpEntity = resellerEmailOtpRepository.findByAdminId(admin.getId())
                .orElseThrow(() -> new BadRequestException("No pending OTP found"));

        if (LocalDateTime.now().isAfter(otpEntity.getExpiresAt())) {
            throw new BadRequestException("OTP has expired");
        }

        if (!otpEntity.getOtpHash().equals(hashOtp(request.getOtp()))) {
            throw new BadRequestException("Invalid OTP");
        }

        admin.setActive(true);
        adminRepository.save(admin);
        resellerEmailOtpRepository.deleteByAdminId(admin.getId());

        return Map.of("success", "true", "message", "Email verified. You can now login.");
    }

    @Override
    @Transactional
    public Map<String, String> resendOtp(String username) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (admin.isActive()) {
            throw new BadRequestException("Account is already verified");
        }

        try {
            sendOtpForUser(admin);
        } catch (Exception e) {
            log.error("Failed to resend OTP for user: {}", username, e);
            throw new BadRequestException("Failed to send OTP. Please try again.");
        }
        return Map.of("success", "true", "message", "OTP resent to " + admin.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(ResellerForgotPasswordRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new AccessDeniedException("Not a reseller account");
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(admin.getEmail());
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        String resetLink = "https://wise-player.com/reset-password?token=" + token;
        emailService.sendResellerPasswordResetEmail(admin.getEmail(), resetLink);
    }

    @Override
    @Transactional
    public AdminAuthResponse resetPassword(ResellerResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Token has expired");
        }

        Admin admin = adminRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
        passwordResetTokenRepository.delete(resetToken);

        return new AdminAuthResponse(true, null, admin.getEmail(), admin.getUsername(), admin.getFullName(),
                admin.getRole().name(), admin.isCanCreate(), admin.isCanRead(), admin.isCanUpdate(), admin.isCanDelete());
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }


    private void sendOtpForUser(Admin admin) {
        String otp = generateOtp();
        resellerEmailOtpRepository.deleteByAdminId(admin.getId());
        ResellerEmailOtp otpEntity = new ResellerEmailOtp();
        otpEntity.setAdminId(admin.getId());
        otpEntity.setOtpHash(hashOtp(otp));
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resellerEmailOtpRepository.save(otpEntity);
        emailService.sendOtpEmail(admin.getEmail(), otp);
        log.info("OTP sent for user: {}", admin.getUsername());
    }


    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateSubResellersBulkPermissions(java.util.UUID resellerId, com.iptv.wiseplayer.dto.request.UpdateResellerRequest request) {
        Admin reseller = adminRepository.findById(resellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        crudPermissionGuard.checkEscalation(reseller, request);

        Boolean canCreate = !reseller.isCanCreate() ? false : request.getCanCreate();
        Boolean canRead = !reseller.isCanRead() ? false : request.getCanRead();
        Boolean canUpdate = !reseller.isCanUpdate() ? false : request.getCanUpdate();
        Boolean canDelete = !reseller.isCanDelete() ? false : request.getCanDelete();

        adminRepository.updatePermissionsByParentId(resellerId, canCreate, canRead, canUpdate, canDelete);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateProfile(UUID adminId, com.iptv.wiseplayer.dto.request.UpdateProfileRequest request) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));
        admin.setFullName(request.getFullName());
        adminRepository.save(admin);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void changePassword(UUID adminId, com.iptv.wiseplayer.dto.request.ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new com.iptv.wiseplayer.exception.BadRequestException("New passwords do not match");
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new com.iptv.wiseplayer.exception.BadRequestException("Invalid current password");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
    }
}
