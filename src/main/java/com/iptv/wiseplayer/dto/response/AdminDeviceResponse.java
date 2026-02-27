package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AdminDeviceResponse {
    private UUID deviceId;
    private String fingerprintHash;
    private DeviceStatus deviceStatus;
    private SubscriptionType subscriptionType;
    private String deviceModel;
    private String osVersion;
    private String platform;
    private LocalDateTime registeredAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime expiresAt;
}
