package com.iptv.wiseplayer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a resource already exists (e.g., email, username).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {
    private String deviceSecret;

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(String message, String deviceSecret) {
        super(message);
        this.deviceSecret = deviceSecret;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }
}
