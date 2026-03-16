package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.ActivationRequestRepository;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import com.iptv.wiseplayer.security.DeviceTokenUtil;
import com.iptv.wiseplayer.service.ResellerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ResellerServiceImpl implements ResellerService {

    private final DeviceRepository deviceRepository;
    private final AdminRepository adminRepository;
    private final ActivationRequestRepository activationRequestRepository;
    private final DeviceTokenUtil tokenUtil;
    private final AdminTokenUtil adminTokenUtil;
    private final PasswordEncoder passwordEncoder;

    public ResellerServiceImpl(DeviceRepository deviceRepository,
            AdminRepository adminRepository,
            ActivationRequestRepository activationRequestRepository,
            DeviceTokenUtil tokenUtil,
            AdminTokenUtil adminTokenUtil,
            PasswordEncoder passwordEncoder) {
        this.deviceRepository = deviceRepository;
        this.adminRepository = adminRepository;
        this.activationRequestRepository = activationRequestRepository;
        this.tokenUtil = tokenUtil;
        this.adminTokenUtil = adminTokenUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Map<String, Object> login(String email, String password) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!admin.isActive()) {
            throw new RuntimeException("Account is disabled");
        }

        // Only allow RESELLER or SUB_RESELLER
        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new RuntimeException("Access denied: Not a reseller account");
        }

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = adminTokenUtil.generateToken(admin.getEmail(), admin.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("username", admin.getEmail());
        response.put("fullName", admin.getUsername());
        response.put("role", admin.getRole().name());
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> register(String email, String password, String fullName) {
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Admin reseller = new Admin();
        reseller.setEmail(email);
        reseller.setPasswordHash(passwordEncoder.encode(password));
        reseller.setUsername(fullName);
        reseller.setRole(AdminRole.RESELLER);
        reseller.setActive(true);

        Admin saved = adminRepository.save(reseller);

        String token = adminTokenUtil.generateToken(saved.getEmail(), saved.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("username", saved.getEmail());
        response.put("fullName", saved.getUsername());
        response.put("role", saved.getRole().name());
        return response;
    }

    @Override
    public ResellerDashboardResponse getDashboardOverview(UUID resellerId) {
        ResellerDashboardResponse response = new ResellerDashboardResponse();
        response.setTotalUsers(deviceRepository.countByResellerId(resellerId));
        response.setActiveSubscriptions(
                deviceRepository.countByResellerIdAndDeviceStatus(resellerId, DeviceStatus.ACTIVE));
        response.setPendingRequests(activationRequestRepository.countByResellerIdAndStatus(resellerId, "PENDING"));
        response.setRecentUsers(deviceRepository.findTop5ByResellerIdOrderByCreatedAtDesc(resellerId));
        return response;
    }

    @Override
    @Transactional
    public DeviceRegistrationResponse createEndUser(UUID resellerId, DeviceRegistrationRequest request) {
        String fingerprintHash = tokenUtil.hashFingerprint(request.getDeviceId());
        if (deviceRepository.findByFingerprintHash(fingerprintHash).isPresent()) {
            throw new RuntimeException("Device already registered");
        }

        Device device = new Device(fingerprintHash, DeviceStatus.INACTIVE);
        device.setSubscriptionType(SubscriptionType.TRIAL);
        device.setDeviceModel(request.getDeviceModel());
        device.setPlatform(request.getPlatform());
        device.setOsVersion(request.getOsVersion());
        device.setResellerId(resellerId);

        String rawSecret = tokenUtil.generateRefreshToken();
        device.setDeviceSecretHash(tokenUtil.hashSecret(rawSecret));

        Device savedDevice = deviceRepository.save(device);

        return new DeviceRegistrationResponse(
                savedDevice.getDeviceId(),
                savedDevice.getDeviceStatus(),
                savedDevice.getSubscriptionType(),
                tokenUtil.generateToken(savedDevice.getDeviceId().toString(), fingerprintHash),
                rawSecret,
                savedDevice.getRegisteredAt());
    }

    @Override
    public List<Device> getResellerUsers(UUID resellerId) {
        return deviceRepository.findAllByResellerId(resellerId);
    }

    @Override
    @Transactional
    public void disableUser(UUID resellerId, UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        if (!resellerId.equals(device.getResellerId())) {
            throw new RuntimeException("Permission denied");
        }
        device.setActive(false);
        device.setDeviceStatus(DeviceStatus.INACTIVE);
        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public Admin createSubReseller(UUID resellerId, String email, String password, String fullName) {
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        Admin sub = new Admin();
        sub.setEmail(email);
        sub.setPasswordHash(passwordEncoder.encode(password));
        sub.setUsername(fullName);
        sub.setRole(AdminRole.SUB_RESELLER);
        sub.setParentId(resellerId);
        sub.setCreatorId(resellerId);
        return adminRepository.save(sub);
    }

    @Override
    public List<Admin> getSubResellers(UUID resellerId) {
        return adminRepository.findAllByParentId(resellerId);
    }

    @Override
    @Transactional
    public ActivationRequest submitActivationRequest(UUID resellerId, UUID deviceId, String planName, String status) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        if (!resellerId.equals(device.getResellerId())) {
            throw new RuntimeException("Permission denied");
        }

        // Determine target status: use parameter if provided, otherwise default to
        // PENDING
        String targetStatus = (status != null && !status.isEmpty()) ? status.toUpperCase() : "PENDING";

        java.util.Optional<ActivationRequest> existingOpt = activationRequestRepository
                .findTopByDeviceIdOrderByCreatedAtDesc(deviceId);
        if (existingOpt.isPresent()) {
            ActivationRequest existing = existingOpt.get();

            // If the status and plan are already the same, block it
            if (targetStatus.equals(existing.getStatus()) && planName.equalsIgnoreCase(existing.getPlanName())) {
                throw new IllegalStateException(
                        "An activation request for this device with status [" + targetStatus + "] already exists");
            }

            // Also block if it's already PENDING and we're trying to submit another PENDING
            if ("PENDING".equals(existing.getStatus()) && "PENDING".equals(targetStatus)) {
                throw new IllegalStateException("An activation request for this device is already pending");
            }

            // Otherwise, update the existing record
            existing.setPlanName(planName);
            existing.setStatus(targetStatus);
            existing.setResellerId(resellerId);
            return activationRequestRepository.save(existing);
        }

        ActivationRequest request = new ActivationRequest();
        request.setResellerId(resellerId);
        request.setDeviceId(deviceId);
        request.setPlanName(planName);
        request.setStatus(targetStatus);
        return activationRequestRepository.save(request);
    }

    @Override
    public List<ActivationRequest> getResellerRequests(UUID resellerId) {
        return activationRequestRepository.findAllByResellerId(resellerId);
    }
}
