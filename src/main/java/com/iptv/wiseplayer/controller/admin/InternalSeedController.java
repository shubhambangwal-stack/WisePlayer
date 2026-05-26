package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.config.DataInitializer;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Super-private internal endpoint for seeding initial data.
 * Protected by a secret header (X-Seed-Secret) that must match the SEED_SECRET environment variable.
 * Hidden from Swagger/OpenAPI docs.
 */
@RestController
@RequestMapping("/api/internal")
@Hidden
public class InternalSeedController {

    private static final Logger log = LoggerFactory.getLogger(InternalSeedController.class);

    private final DataInitializer dataInitializer;
    private final String seedSecret;

    public InternalSeedController(DataInitializer dataInitializer,
                                  @Value("${app.security.seed-secret}") String seedSecret) {
        this.dataInitializer = dataInitializer;
        this.seedSecret = seedSecret;
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedData(
            @RequestHeader("X-Seed-Secret") String providedSecret) {

        Map<String, Object> response = new HashMap<>();

        // Validate the seed secret
        if (!seedSecret.equals(providedSecret)) {
            log.warn("Unauthorized seed attempt with invalid secret");
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        log.info("Seed endpoint triggered with valid secret");
        List<String> results = dataInitializer.seedData();

        response.put("success", true);
        response.put("message", "Seed operation completed");
        response.put("details", results);
        return ResponseEntity.ok(response);
    }
}
