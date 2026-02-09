package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.domain.entity.Device;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class DeviceAuthenticationToken extends AbstractAuthenticationToken {

    private final Device device;

    public DeviceAuthenticationToken(Device device) {
        super(null);
        this.device = device;
        setAuthenticated(true);
    }

    public DeviceAuthenticationToken(Device device, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.device = device;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return device;
    }

    public Device getDevice() {
        return device;
    }
}
