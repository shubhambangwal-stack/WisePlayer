package com.iptv.wiseplayer.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across all APIs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(DeviceAuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleDeviceAuthenticationException(DeviceAuthenticationException ex) {
                log.warn("Device Authentication Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        @ExceptionHandler(DeviceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleDeviceNotFoundException(DeviceNotFoundException ex) {
                log.warn("Device Not Found Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }

        @ExceptionHandler(InvalidFingerprintException.class)
        public ResponseEntity<ErrorResponse> handleInvalidFingerprintException(InvalidFingerprintException ex) {
                log.warn("Invalid Fingerprint Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
                log.warn("Validation Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));
                log.warn("Validation Error: {}", message);
                return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
                log.error("Unexpected Error: ", ex);
                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }

        private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
                ErrorResponse response = new ErrorResponse(status.value(), message);
                return new ResponseEntity<>(response, status);
        }
}
