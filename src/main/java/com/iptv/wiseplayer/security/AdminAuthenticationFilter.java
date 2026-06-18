package com.iptv.wiseplayer.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iptv.wiseplayer.repository.AdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AdminAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthenticationFilter.class);
    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminTokenUtil adminTokenUtil;
    private final ObjectMapper objectMapper;
    private final AdminRepository adminRepository;

    public AdminAuthenticationFilter(AdminTokenUtil adminTokenUtil, ObjectMapper objectMapper,
                                     AdminRepository adminRepository) {
        this.adminTokenUtil = adminTokenUtil;
        this.objectMapper = objectMapper;
        this.adminRepository = adminRepository;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip filtering for public auth endpoints
        if (path.equals("/api/admin/auth/login") ||
                path.equals("/api/admin/auth/forgot-password") ||
                path.equals("/api/admin/auth/reset-password") ||
                path.equals("/api/reseller/login") ||
                path.equals("/api/reseller/register") ||
                path.equals("/api/reseller/forgot-password") ||
                path.equals("/api/reseller/reset-password") ||
                path.equals("/api/admin/management/invite/verify") ||
                path.equals("/api/admin/management/setup/complete") ||
                path.equals("/wp-admin-monitor/health")) {
            return true;
        }
        // Only filter admin, reseller, monitoring and documentation API paths
        return !(path.startsWith("/api/admin") || path.startsWith("/api/reseller")
                || path.startsWith("/api/sub-reseller")
                || path.startsWith("/wp-admin-monitor")
                || path.startsWith("/wp-docs")
                || path.startsWith("/wp-api-spec"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Accept X-Admin-Token header OR standard Authorization: Bearer <token>
        String token = request.getHeader(ADMIN_TOKEN_HEADER);
        if (token == null || token.isEmpty()) {
            String bearerHeader = request.getHeader("Authorization");
            if (bearerHeader != null && bearerHeader.startsWith(BEARER_PREFIX)) {
                token = bearerHeader.substring(BEARER_PREFIX.length());
            }
        }

        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            java.util.Map<String, Object> claims = adminTokenUtil.verifyAndExtractClaims(token);
            String username = (String) claims.get("username");
            String role = (String) claims.get("role");
            String method = request.getMethod();
            String path = request.getRequestURI();

            if (!"SUPER_ADMIN".equals(role)) {
                com.iptv.wiseplayer.domain.entity.Admin admin =
                        adminRepository.findByUsername(username).orElse(null);

                if (admin != null) {
                    boolean blocked = false;
                    String reason = "";

                    switch (method.toUpperCase()) {
                        case "GET":
                            if (!admin.isCanRead()) { blocked = true; reason = "Access Denied: Read permission revoked."; }
                            break;
                        case "POST":
                            if (!admin.isCanCreate()) { blocked = true; reason = "Access Denied: Create permission revoked."; }
                            break;
                        case "PUT":
                        case "PATCH":
                            if (!admin.isCanUpdate()) { blocked = true; reason = "Access Denied: Update permission revoked."; }
                            break;
                        case "DELETE":
                            if (!admin.isCanDelete()) { blocked = true; reason = "Access Denied: Delete permission revoked."; }
                            break;
                    }

                    if (blocked) {
                        log.warn("RBAC block: username={}, role={}, method={}, path={}", username, role, method, path);
                        handleForbiddenAuthenticationFailure(response, reason);
                        return;
                    }
                }
            }

            log.info("Admin authenticated: username={}, role={}, path={}", username, role, path);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("Admin authentication failed: {}", e.getMessage());
            handleAuthenticationFailure(response, "Invalid or expired admin token");
        }
    }


    private void handleForbiddenAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("success", false);
        errorDetails.put("message", message);
        errorDetails.put("status", 403);
        errorDetails.put("error", "PERMISSION_DENIED");
        errorDetails.put("action", "LOGOUT");

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
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
