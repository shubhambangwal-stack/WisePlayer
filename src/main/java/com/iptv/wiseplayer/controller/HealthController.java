package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.config.SecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Public endpoint to verify API availability")
public class HealthController {

    private final SecurityProperties securityProperties;

    public HealthController(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Operation(summary = "Health Check", description = "Returns server status, uptime, timestamp, and active token TTL configuration.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("uptime", String.format("%dd %dh %dm %ds",
                uptime.toDays(),
                uptime.toHoursPart(),
                uptime.toMinutesPart(),
                uptime.toSecondsPart()));
        response.put("tokenTtlMinutes", securityProperties.getTokenTtlMinutes());

        return ResponseEntity.ok(response);
    }
}
