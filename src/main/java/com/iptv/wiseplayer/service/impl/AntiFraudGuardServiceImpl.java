package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.exception.ConnectionLimitException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.service.AntiFraudGuardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AntiFraudGuardServiceImpl implements AntiFraudGuardService {

    private static final Logger log = LoggerFactory.getLogger(AntiFraudGuardServiceImpl.class);
    private final DeviceRepository deviceRepository;

    // A thread-safe local fallback cache to simulate active stream counts
    private final Map<UUID, Integer> localActiveStreams = new ConcurrentHashMap<>();

    public AntiFraudGuardServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void checkConnectionLimit(UUID deviceId) {
        // 1. Fetch device to determine allowed limit
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Determine max allowed connections based on subscription type
        int maxConnections = 3; // Default for active/premium devices
        if ("TRIAL".equalsIgnoreCase(device.getPlanName())) {
            maxConnections = 1; // Trials are restricted to a single stream
        }

        log.debug("Checking concurrent stream limit for device {}. Max allowed: {}", deviceId, maxConnections);

        // 2. Logical Hook Placeholder for Redis counter:
        /*
         * In a multi-instance/production environment:
         * 
         * String redisKey = "device:active_streams:" + deviceId;
         * Long activeCount = redisTemplate.opsForValue().get(redisKey);
         * if (activeCount == null) {
         *     activeCount = 0L;
         * }
         * 
         * // Check if threshold exceeded
         * if (activeCount >= maxConnections) {
         *     throw new ConnectionLimitException(activeCount.intValue(), maxConnections);
         * }
         * 
         * // If checked during play start, increment counter:
         * // redisTemplate.opsForValue().increment(redisKey);
         * // redisTemplate.expire(redisKey, Duration.ofHours(3)); // self-healing TTL
         */

        // For local demo/testing, simulate checking our concurrent hashmap:
        int activeCount = localActiveStreams.getOrDefault(deviceId, 0);

        if (activeCount >= maxConnections) {
            log.warn("Device {} reached concurrent stream limit: {}/{}", deviceId, activeCount, maxConnections);
            throw new ConnectionLimitException(activeCount, maxConnections);
        }
    }

    // Helper methods to simulate stream start and end (used for testing/lifecycle)
    public void recordStreamStart(UUID deviceId) {
        localActiveStreams.merge(deviceId, 1, Integer::sum);
    }

    public void recordStreamEnd(UUID deviceId) {
        localActiveStreams.computeIfPresent(deviceId, (k, v) -> v > 1 ? v - 1 : null);
    }
}
