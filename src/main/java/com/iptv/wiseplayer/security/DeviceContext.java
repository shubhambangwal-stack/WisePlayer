package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.domain.entity.Device;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeviceContext {

    public Device getCurrentDevice() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof DeviceAuthenticationToken) {
            return ((DeviceAuthenticationToken) authentication).getDevice();
        }
        return null;
    }

    public UUID getCurrentDeviceId() {
        Device device = getCurrentDevice();
        return device != null ? device.getDeviceId() : null;
    }
}
