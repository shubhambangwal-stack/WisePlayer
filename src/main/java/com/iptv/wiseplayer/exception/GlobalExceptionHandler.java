package com.iptv.wiseplayer.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
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
        public ResponseEntity<ErrorResponse> handleDeviceAuthenticationException(DeviceAuthenticationException ex,
                        HttpServletRequest request) {
                log.warn("Device Authentication Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
        }

        @ExceptionHandler(DeviceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleDeviceNotFoundException(DeviceNotFoundException ex,
                        HttpServletRequest request) {
                log.warn("Device Not Found Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        }

        @ExceptionHandler(InvalidFingerprintException.class)
        public ResponseEntity<ErrorResponse> handleInvalidFingerprintException(InvalidFingerprintException ex,
                        HttpServletRequest request) {
                log.warn("Invalid Fingerprint Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
                        HttpServletRequest request) {
                log.warn("Validation Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex,
                        HttpServletRequest request) {
                log.warn("Bad Request: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex,
                        HttpServletRequest request) {
                log.warn("Resource Not Found: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        }

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex,
                        HttpServletRequest request) {
                log.warn("Resource Already Exists: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
        }

        @ExceptionHandler(InvalidInvitationException.class)
        public ResponseEntity<ErrorResponse> handleInvalidInvitationException(InvalidInvitationException ex,
                        HttpServletRequest request) {
                log.warn("Invalid Invitation: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
                        HttpServletRequest request) {
                log.warn("Authentication Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
                        HttpServletRequest request) {
                log.warn("Access Denied: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.FORBIDDEN,
                                "Access Denied: You do not have permission to access this resource", request);
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex,
                        HttpServletRequest request) {
                log.warn("State Conflict Error: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));
                log.warn("Validation Error: {}", message);
                return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex,
                        HttpServletRequest request) {
                log.warn("Data Integrity Violation: {}", ex.getMessage());
                String message = "Resource already exists or database constraint violation";
                if (ex.getRootCause() != null && ex.getRootCause().getMessage().contains("duplicate key")) {
                        message = "A resource with this identifier already exists (e.g., email or name)";
                }
                return buildErrorResponse(HttpStatus.CONFLICT, message, request);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
                log.error("Unexpected Error: ", ex);
                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
        }

        private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(status.value(), message, request.getRequestURI());
                return new ResponseEntity<>(response, status);
        }
}
