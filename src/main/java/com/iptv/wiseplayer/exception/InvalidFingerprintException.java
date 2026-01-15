package com.iptv.wiseplayer.exception;

/**
 * Exception thrown when device fingerprint validation fails.
 */
public class InvalidFingerprintException extends RuntimeException {

    public InvalidFingerprintException(String message) {
        super(message);
    }

    public InvalidFingerprintException(String message, Throwable cause) {
        super(message, cause);
    }
}
