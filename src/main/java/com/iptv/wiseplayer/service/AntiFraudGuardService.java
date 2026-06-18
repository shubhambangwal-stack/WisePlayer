package com.iptv.wiseplayer.service;

import java.util.UUID;

/**
 * Service to protect against abuse and enforce limits, such as checking concurrent connection limits.
 */
public interface AntiFraudGuardService {

    /**
     * Checks if the device has exceeded the maximum allowed concurrent streams.
     * Throws ConnectionLimitException if the limit is exceeded.
     *
     * @param deviceId the ID of the device
     */
    void checkConnectionLimit(UUID deviceId);
}
