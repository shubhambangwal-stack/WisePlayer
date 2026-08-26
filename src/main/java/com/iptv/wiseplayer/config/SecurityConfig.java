package com.iptv.wiseplayer.config;

import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.security.AdminAuthenticationFilter;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import com.iptv.wiseplayer.security.DeviceAuthenticationFilter;
import com.iptv.wiseplayer.security.DeviceTokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final com.iptv.wiseplayer.repository.DeviceRepository deviceRepository;
    private final DeviceTokenUtil deviceTokenUtil;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final AdminRepository adminRepository;

    public SecurityConfig(SecurityProperties securityProperties,
            DeviceRepository deviceRepository,
            DeviceTokenUtil deviceTokenUtil,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            AdminRepository adminRepository) {
        this.securityProperties = securityProperties;
        this.deviceRepository = deviceRepository;
        this.deviceTokenUtil = deviceTokenUtil;
        this.objectMapper = objectMapper;
        this.adminRepository = adminRepository;
    }

    @Bean
    public AdminTokenUtil adminTokenUtil() {
        return new AdminTokenUtil(securityProperties);
    }

    @Bean
    public AdminAuthenticationFilter adminAuthenticationFilter() {
        return new AdminAuthenticationFilter(adminTokenUtil(), objectMapper, adminRepository);
    }

    @Bean
    public DeviceAuthenticationFilter deviceAuthenticationFilter() {
        return new DeviceAuthenticationFilter(deviceTokenUtil, deviceRepository, objectMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setExposedHeaders(
                java.util.List.of("Authorization", "Link", "X-Total-Count", "ngrok-skip-browser-warning"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            DeviceAuthenticationFilter deviceAuthenticationFilter,
            AdminAuthenticationFilter adminAuthenticationFilter) throws Exception {
        http
                // Enable CORS using the configured source
                 .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF using the new lambda style
                .csrf(AbstractHttpConfigurer::disable)

                // Configure session management to stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        // Allow async dispatches to bypass security filter chain
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC).permitAll()

                        .requestMatchers("/api/device/register").permitAll()
                        .requestMatchers("/api/device/validate").permitAll()
                        .requestMatchers("/api/device/refresh").permitAll()
                        .requestMatchers("/api/device/key").permitAll()
                        .requestMatchers("/api/device/activate").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/playlist/public/**").permitAll()
                        .requestMatchers("/api/payment/paypal/**").permitAll()
                        .requestMatchers("/api/reseller/login", "/api/reseller/register",
                                "/api/reseller/forgot-password", "/api/reseller/reset-password")
                        .permitAll()
                        .requestMatchers("/api/payment/public/**").permitAll()
                        .requestMatchers("/api/payment/public/plans").permitAll()
                        .requestMatchers("/api/public/support/**").permitAll()
                        .requestMatchers("/api/internal/seed").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // Monitoring & Docs (Health is public, others are restricted)
                        .requestMatchers("/wp-admin-monitor/health").permitAll()
                        .requestMatchers("/wp-api-spec/**", "/wp-docs-assets/**", "/wp-docs/**", "/wp-admin-monitor/**")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        // Admin Endpoints
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .requestMatchers("/api/admin/auth/forgot-password").permitAll()
                        .requestMatchers("/api/admin/auth/reset-password").permitAll()
                        .requestMatchers("/api/admin/management/invite/verify").permitAll()
                        .requestMatchers("/api/admin/management/setup/complete").permitAll()
                        .requestMatchers("/api/admin/management/**").hasAuthority("ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/admin/resellers/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/admin/plans/active")
                        .hasAnyAuthority("ROLE_RESELLER", "ROLE_SUB_RESELLER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/admin/plans/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/admin/activation-requests/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers("/api/admin/reports/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                        // Any authenticated admin-type role can view their own CRUD permissions view
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/admin/crud-permissions")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_RESELLER", "ROLE_SUB_RESELLER")
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                        // Reseller Endpoints
                        .requestMatchers("/api/reseller/verify-email", "/api/reseller/resend-otp")
                        .hasAnyAuthority("ROLE_RESELLER", "ROLE_SUB_RESELLER")
                        .requestMatchers("/api/reseller/**")
                        .hasAuthority("ROLE_RESELLER")

                        // Sub-Reseller Endpoints
                        .requestMatchers("/api/sub-reseller/**")
                        .hasAuthority("ROLE_SUB_RESELLER")

                        // Protected Endpoints (Require Device Token)
                        .requestMatchers("/api/payment/checkout").authenticated()
                        .requestMatchers("/api/subscription/**").authenticated()
                        .requestMatchers("/api/device/key/status").authenticated()

                        // Content Endpoints (Require ACTIVE status)
                        .requestMatchers("/api/playlist", "/api/playlist/**").hasRole("ACTIVE")
                        .requestMatchers("/api/live/**").hasRole("ACTIVE")
                        .requestMatchers("/api/stream/**", "/api/v2/stream/**").hasRole("ACTIVE")
                        .requestMatchers("/api/catchup/**").hasRole("ACTIVE")
                        .requestMatchers("/api/xtream/**").hasRole("ACTIVE")

                        .anyRequest().authenticated())

                // Register filters
                .addFilterBefore(deviceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(org.springframework.security.config.Customizer.withDefaults());

        return http.build();
    }
}
