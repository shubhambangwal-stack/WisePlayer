package com.iptv.wiseplayer.exception;

import org.springframework.security.core.AuthenticationException;

public class DeviceAuthenticationException extends AuthenticationException {
    public DeviceAuthenticationException(String msg) {
        super(msg);
    }

    public DeviceAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
