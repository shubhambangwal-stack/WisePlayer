package com.iptv.wiseplayer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Type-safe configuration properties for application security.
 * Binds properties prefixed with 'app.security' from application files.
 */
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * List of secrets used for HMAC-SHA256 token signing and rotation.
     * The first secret in the list is always used for signing new tokens.
     */
    private List<String> tokenSecrets;

    /**
     * Duration in minutes for which an access token remains valid.
     */
    private long tokenTtlMinutes;

    /**
     * CORS Configuration: List of allowed origins.
     */
    private List<String> allowedOrigins;

    /**
     * CORS Configuration: List of allowed HTTP methods.
     */
    private List<String> allowedMethods;

    /**
     * CORS Configuration: List of allowed headers.
     */
    private List<String> allowedHeaders;

    /**
     * CORS Configuration: Whether to allow credentials (cookies, auth headers).
     */
    private boolean allowCredentials = true;

    public List<String> getTokenSecrets() {
        return tokenSecrets;
    }

    public void setTokenSecrets(List<String> tokenSecrets) {
        this.tokenSecrets = tokenSecrets;
    }

    public long getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }

    public void setTokenTtlMinutes(long tokenTtlMinutes) {
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
