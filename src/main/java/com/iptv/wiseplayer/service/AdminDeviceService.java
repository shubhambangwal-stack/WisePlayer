package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.dto.response.AdminDeviceResponse;
import com.iptv.wiseplayer.repository.DeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminDeviceService {

    private final DeviceRepository deviceRepository;

    public AdminDeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Page<AdminDeviceResponse> getAllDevices(Pageable pageable) {
        return deviceRepository.findAll(pageable).map(this::convertToResponse);
    }

    public AdminDeviceResponse getDeviceById(UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return convertToResponse(device);
    }

    @Transactional
    public void updateDeviceStatus(UUID deviceId, DeviceStatus status) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        device.setDeviceStatus(status);
        deviceRepository.save(device);
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
