package com.iptv.wiseplayer.config;

import com.iptv.wiseplayer.security.DeviceAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DeviceAuthenticationFilter deviceAuthenticationFilter;

    public SecurityConfig(DeviceAuthenticationFilter deviceAuthenticationFilter) {
        this.deviceAuthenticationFilter = deviceAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF using the new lambda style
                .csrf(AbstractHttpConfigurer::disable)

                // Configure session management to stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/api/device/register").permitAll()
                        .requestMatchers("/api/device/key").permitAll()
                        .requestMatchers("/api/device/activate").permitAll()
                        .requestMatchers("/api/payment/**").permitAll()

                        // Protected Endpoints (Require Device Token)
                        .requestMatchers("/api/device/validate").authenticated()
                        .requestMatchers("/api/playlist/**").authenticated()
                        .requestMatchers("/api/subscription/status").authenticated()
                        .requestMatchers("/api/device/key/status").authenticated()

                        .anyRequest().authenticated())

                // Register custom device authentication filter
                .addFilterBefore(deviceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
