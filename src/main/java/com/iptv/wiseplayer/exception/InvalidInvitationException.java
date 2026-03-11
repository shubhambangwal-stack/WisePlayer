package com.iptv.wiseplayer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown for invitation-related errors (already sent, already used).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInvitationException extends RuntimeException {
    public InvalidInvitationException(String message) {
        super(message);
    }
}
