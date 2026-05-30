package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import java.math.BigDecimal;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.ResellerActivationRequestDto;
import com.iptv.wiseplayer.dto.request.ResellerLoginRequest;
import com.iptv.wiseplayer.dto.request.ResellerRegisterRequest;
import com.iptv.wiseplayer.dto.request.SubResellerCreateRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.AuthenticationException;
import com.iptv.wiseplayer.exception.ResourceAlreadyExistsException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.ActivationRequestRepository;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import com.iptv.wiseplayer.security.DeviceTokenUtil;
import com.iptv.wiseplayer.service.ResellerService;
import org.slf4j.Logger;


import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public ResellerServiceImpl(DeviceRepository deviceRepository,
            AdminRepository adminRepository,
            ActivationRequestRepository activationRequestRepository,
            DeviceTokenUtil tokenUtil,
            AdminTokenUtil adminTokenUtil,
            PasswordEncoder passwordEncoder,
            com.iptv.wiseplayer.service.CreditService creditService,
            com.iptv.wiseplayer.repository.SubscriptionRepository subscriptionRepository,
            com.iptv.wiseplayer.repository.ResellerCustomerRepository resellerCustomerRepository) {
        this.deviceRepository = deviceRepository;
        this.adminRepository = adminRepository;
        this.activationRequestRepository = activationRequestRepository;
        this.tokenUtil = tokenUtil;
        this.adminTokenUtil = adminTokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.creditService = creditService;
        this.subscriptionRepository = subscriptionRepository;
        this.resellerCustomerRepository = resellerCustomerRepository;
    }



    @Override
    public AdminAuthResponse login(ResellerLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!admin.isActive()) {
            throw new AccessDeniedException("Account is disabled");
        }

        // Only allow RESELLER or SUB_RESELLER
        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new AccessDeniedException("Access denied: Not a reseller account");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        String token = adminTokenUtil.generateToken(admin.getUsername(), admin.getRole());

        return new AdminAuthResponse(true, token, null, admin.getUsername(), admin.getFullName(),
                admin.getRole().name());
    }

    @Override
    @Transactional
    public AdminAuthResponse register(ResellerRegisterRequest request) {
        if (adminRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        Admin reseller = new Admin();
        reseller.setUsername(request.getUsername());
        reseller.setFullName(request.getFullName());
        reseller.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        reseller.setRole(AdminRole.RESELLER);
        reseller.setActive(true);

        Admin saved = adminRepository.save(reseller);

        String token = adminTokenUtil.generateToken(saved.getUsername(), saved.getRole());

        return new AdminAuthResponse(true, token, null, saved.getUsername(), saved.getFullName(),
                saved.getRole().name());
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

    @Override
    public org.springframework.data.domain.Page<Device> getResellerUsers(UUID resellerId,
            String search,
            com.iptv.wiseplayer.domain.enums.DeviceStatus status,
            org.springframework.data.domain.Pageable pageable) {
        return deviceRepository.searchResellerUsers(resellerId, status, search, pageable);
    }



    @Override
    @Transactional
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
    public Admin createSubReseller(UUID resellerId, SubResellerCreateRequest request) {
        if (adminRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResourceAlreadyExistsException("Username already exists");
        }
        Admin sub = new Admin();
        sub .setUsername(request.getUsername());
        sub.setFullName(request.getFullName());
        sub.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        sub.setRole(AdminRole.SUB_RESELLER);
        sub.setParentId(resellerId);
        sub.setCreatorId(resellerId);
        return adminRepository.save(sub);
    }

    @Override
    public org.springframework.data.domain.Page<Admin> getSubResellers(
            UUID resellerId, String search, Boolean status,
            org.springframework.data.domain.Pageable pageable) {
        return adminRepository.searchSubResellers(resellerId, search, status, pageable);
    }

    @Override
    @Transactional
    public void updateSubReseller(UUID resellerId, UUID subResellerId,
            com.iptv.wiseplayer.dto.request.SubResellerUpdateRequest request) {
        Admin sub = adminRepository.findById(subResellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-reseller not found"));

        if (!resellerId.equals(sub.getParentId())) {
            throw new AccessDeniedException("Permission denied: Not your sub-reseller");
        }

        sub.setFullName(request.getFullName());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            sub.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        adminRepository.save(sub);
    }

    @Override
    @Transactional
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
    public org.springframework.data.domain.Page<com.iptv.wiseplayer.dto.response.ActivationRequestResponse> getResellerRequests(
            UUID resellerId, String search, String status, String planName, org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.domain.Page<ActivationRequest> requestsPage =
                activationRequestRepository.searchResellerRequests(resellerId, status, planName, search, pageable);
        
        return requestsPage.map(request -> {
                    com.iptv.wiseplayer.dto.response.ActivationRequestResponse response = new com.iptv.wiseplayer.dto.response.ActivationRequestResponse();
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

                    // Fetch device details
                    deviceRepository.findByDeviceId(request.getDeviceId()).ifPresent(device -> {
                        response.setDeviceModel(device.getDeviceModel());
                        response.setPlatform(device.getPlatform());
                        response.setDeviceStatus(device.getDeviceStatus().name());
                        response.setMacAddress(device.getMacAddress());
                    });



                    return response;
                });
    }
}
