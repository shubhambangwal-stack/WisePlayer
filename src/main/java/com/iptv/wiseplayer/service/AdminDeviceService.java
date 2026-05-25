package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.dto.response.AdminDeviceResponse;
import com.iptv.wiseplayer.repository.DeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class AdminDeviceService {

    private final DeviceRepository deviceRepository;

    public AdminDeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Page<AdminDeviceResponse> getAllDevices(
            String deviceId,
            DeviceStatus status,
            SubscriptionType subscription,
            String model,
            String platform,
            Pageable pageable) {
        return deviceRepository.searchDevices(deviceId, status, subscription, model, platform, pageable)
                .map(this::convertToResponse);
    }

    public AdminDeviceResponse getDeviceByIdOrMac(String idOrMac) {
        Device device = findDeviceByIdentifier(idOrMac);
        return convertToResponse(device);
    }

    @Transactional
    public void updateDeviceStatus(String idOrMac, DeviceStatus status) {
        Device device = findDeviceByIdentifier(idOrMac);
        device.setDeviceStatus(status);
        deviceRepository.save(device);
    }

    private Device findDeviceByIdentifier(String identifier) {
        try {
            // Try parsing as UUID first
            UUID id = UUID.fromString(identifier);
            return deviceRepository.findByDeviceId(id)
                    .orElseThrow(() -> new RuntimeException("Device not found by ID"));
        } catch (IllegalArgumentException e) {
            // If not a UUID, treat as MAC address and hash it
            String hash = hashMacAddress(identifier);
            return deviceRepository.findByFingerprintHash(hash)
                    .orElseThrow(() -> new RuntimeException("Device not found by MAC address"));
        }
    }

    private String hashMacAddress(String mac) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(mac.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash MAC address", e);
        }
    }

    private AdminDeviceResponse convertToResponse(Device device) {
        AdminDeviceResponse response = new AdminDeviceResponse();
        response.setDeviceId(device.getDeviceId());
        response.setFingerprintHash(device.getFingerprintHash());
        response.setDeviceStatus(device.getDeviceStatus());
        response.setSubscriptionType(device.getSubscriptionType());
        response.setDeviceModel(device.getDeviceModel());
        response.setOsVersion(device.getOsVersion());
        response.setPlatform(device.getPlatform());
        response.setRegisteredAt(device.getRegisteredAt());
        response.setLastSeenAt(device.getLastSeenAt());
        response.setExpiresAt(device.getExpiresAt());
        return response;
    }
}
