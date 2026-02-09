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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip filter for non-api or public endpoints if necessary,
        // but SecurityConfig handles the mapping. We only validate if headers are
        // present
        // OR if path matches protected patterns.

        String token = request.getHeader(TOKEN_HEADER);
        String fingerprint = request.getHeader(FINGERPRINT_HEADER);

        if (token == null || fingerprint == null) {
            // Let it pass to SecurityConfig which will reject if authenticated is required
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Verify token and fingerprint match
            Map<String, String> claims = tokenUtil.verifyAndExtract(token, fingerprint);
            UUID deviceId = UUID.fromString(claims.get("deviceId"));

            // 2. Load device and verify status
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new DeviceAuthenticationException("Device not found"));

            if (device.getDeviceStatus() == DeviceStatus.BLOCKED) {
                throw new DeviceAuthenticationException("Device is blocked");
            }

            // 3. Set authentication context with status-based roles
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_DEVICE"));
            authorities.add(new SimpleGrantedAuthority("ROLE_" + device.getDeviceStatus().name()));

            DeviceAuthenticationToken authentication = new DeviceAuthenticationToken(device, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (DeviceAuthenticationException e) {
            log.warn("Device authentication failed for path {}: {}", path, e.getMessage());
            handleAuthenticationFailure(response, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected security filter error", e);
            handleAuthenticationFailure(response, "Internal security error");
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
