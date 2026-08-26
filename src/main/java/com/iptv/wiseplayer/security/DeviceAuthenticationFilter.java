package com.iptv.wiseplayer.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.exception.DeviceAuthenticationException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeviceAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceAuthenticationFilter.class);
    private static final String TOKEN_HEADER = "X-Device-Token";
    private static final String FINGERPRINT_HEADER = "X-Device-Fingerprint";

    private final DeviceTokenUtil tokenUtil;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    public DeviceAuthenticationFilter(DeviceTokenUtil tokenUtil,
            DeviceRepository deviceRepository,
            ObjectMapper objectMapper) {
        this.tokenUtil = tokenUtil;
        this.deviceRepository = deviceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean skip = path.startsWith("/api/payment/paypal/") ||
                path.startsWith("/api/payment/public/") ||
                path.startsWith("/api/device/register") ||
                path.startsWith("/api/device/validate") ||
                path.startsWith("/api/device/refresh") ||
                (path.equals("/api/device/key") && "POST".equalsIgnoreCase(request.getMethod())) ||
                path.startsWith("/api/device/activate") ||
                path.startsWith("/api/playlist/public/") ||
                path.startsWith("/api/reseller/login") ||
                path.startsWith("/api/reseller/register") ||
                path.startsWith("/api/admin/auth/login") ||
                path.startsWith("/api/health") ||
                path.startsWith("/wp-api-spec") ||
                path.startsWith("/wp-docs") ||
                path.startsWith("/wp-docs-assets") ||
                path.startsWith("/swagger-ui");
        if (skip) {
            log.debug("[DeviceFilter] SKIPPING filter for public path: {}", path);
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        log.info("[DeviceFilter] >>> Incoming request: {} {}", method, path);

        // Log ALL incoming headers to see what the server actually receives
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = request.getHeader(name);
                if (name.equalsIgnoreCase(TOKEN_HEADER)) {
                    log.info("[DeviceFilter] Header present: {} (length={})", name, value != null ? value.length() : 0);
                } else if (name.equalsIgnoreCase(FINGERPRINT_HEADER)) {
                    log.info("[DeviceFilter] Header present: {} (length={})", name, value != null ? value.length() : 0);
                } else {
                    log.debug("[DeviceFilter] Header: {} = {}", name, value);
                }
            }
        }

        String token = request.getHeader(TOKEN_HEADER);
        if (token == null) {
            log.info("[DeviceFilter] X-Device-Token header NOT found. Trying query param 'token'...");
            token = request.getParameter("token");
            if (token != null) {
                log.info("[DeviceFilter] Found token in query param. length={}", token.length());
            }
        }

        String fingerprint = request.getHeader(FINGERPRINT_HEADER);
        if (fingerprint == null) {
            log.info("[DeviceFilter] X-Device-Fingerprint header NOT found. Trying query param 'fingerprint'...");
            fingerprint = request.getParameter("fingerprint");
            if (fingerprint != null) {
                log.info("[DeviceFilter] Found fingerprint in query param. length={}", fingerprint.length());
            }
        }

        if (token == null) {
            log.warn("[DeviceFilter] !! X-Device-Token is NULL for path '{}'. Passing to Spring Security — route will 401 if it requires ROLE_ACTIVE.", path);
        }
        if (fingerprint == null) {
            log.warn("[DeviceFilter] !! X-Device-Fingerprint is NULL for path '{}'. Passing to Spring Security — route will 401 if it requires ROLE_ACTIVE.", path);
        }

        if (token == null || fingerprint == null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("[DeviceFilter] STEP 1 OK: Both token and fingerprint present. Verifying...");

        try {
            // 1. Verify token signature + fingerprint
            log.info("[DeviceFilter] STEP 2: Verifying token signature and fingerprint hash...");
            Map<String, String> claims = tokenUtil.verifyAndExtract(token, fingerprint);
            String rawDeviceId = claims.get("deviceId");
            log.info("[DeviceFilter] STEP 2 OK: Token verified successfully. deviceId={}", rawDeviceId);

            UUID deviceId = UUID.fromString(rawDeviceId);

            // 2. Load device from DB
            log.info("[DeviceFilter] STEP 3: Loading device from DB. deviceId={}", deviceId);
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new DeviceAuthenticationException("Device not found in DB for id: " + deviceId));
            log.info("[DeviceFilter] STEP 3 OK: Device found. status={}, expiresAt={}", device.getDeviceStatus(), device.getExpiresAt());

            // 3. Check expiry
            boolean isExpired = device.getDeviceStatus() == DeviceStatus.ACTIVE &&
                    device.getExpiresAt() != null &&
                    LocalDateTime.now().isAfter(device.getExpiresAt());
            log.info("[DeviceFilter] STEP 4: Expiry check — deviceStatus={}, isExpired={}, now={}, expiresAt={}",
                    device.getDeviceStatus(), isExpired, LocalDateTime.now(), device.getExpiresAt());

            // 4. Build roles
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_DEVICE"));

            if (device.getDeviceStatus() == DeviceStatus.ACTIVE && !isExpired) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ACTIVE"));
                log.info("[DeviceFilter] STEP 5 OK: Device is ACTIVE and not expired. Granted ROLE_ACTIVE.");
            } else {
                String statusRole = "ROLE_" + device.getDeviceStatus().name();
                authorities.add(new SimpleGrantedAuthority(statusRole));
                if (isExpired) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_EXPIRED"));
                }
                log.warn("[DeviceFilter] STEP 5 WARNING: Device does NOT have ROLE_ACTIVE. " +
                        "Granted roles: {}. " +
                        "This device will be blocked from /api/playlist and other ACTIVE-only endpoints.", authorities);
            }

            DeviceAuthenticationToken authentication = new DeviceAuthenticationToken(device, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("[DeviceFilter] STEP 6 OK: SecurityContext populated with roles: {}. Handing off to controller.", authorities);

            filterChain.doFilter(request, response);

        } catch (DeviceAuthenticationException e) {
            log.warn("[DeviceFilter] AUTHENTICATION FAILED — {} {}: {}", method, path, e.getMessage());
            handleAuthenticationFailure(response, e.getMessage());
        } catch (Exception e) {
            log.error("[DeviceFilter] UNEXPECTED ERROR — {} {}: {}", method, path, e.getMessage(), e);
            handleAuthenticationFailure(response, "Internal security error: " + e.getMessage());
        }
    }

    private void handleAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("success", false);
        errorDetails.put("message", message);
        errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}
