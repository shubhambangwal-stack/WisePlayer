package com.iptv.wiseplayer.exception;

/**
 * Exception thrown when a device is not found in the system.
 */
public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String message) {
        super(message);
    }

    public DeviceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
