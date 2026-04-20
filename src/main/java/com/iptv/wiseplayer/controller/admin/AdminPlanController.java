package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.request.PlanRequest;
import com.iptv.wiseplayer.dto.response.PlanResponse;
import com.iptv.wiseplayer.service.AdminPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/plans")
@Tag(name = "Admin Plan Management", description = "Endpoints for managing subscription plans")
public class AdminPlanController {

    private final AdminPlanService adminPlanService;

    public AdminPlanController(AdminPlanService adminPlanService) {
        this.adminPlanService = adminPlanService;
    }

    @Operation(summary = "Create Plan", description = "Creates a new subscription plan. Only accessible by ADMIN/SUPER_ADMIN.")
    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminPlanService.createPlan(request));
    }

    @Operation(summary = "List All Plans", description = "Retrieves a paginated list of all subscription plans.")
    @GetMapping
    public ResponseEntity<Page<PlanResponse>> getAllPlans(Pageable pageable) {
        return ResponseEntity.ok(adminPlanService.getAllPlans(pageable));
    }

    @Operation(summary = "List Active Plans", description = "Retrieves a list of all active subscription plans.")
    @GetMapping("/active")
    public ResponseEntity<List<PlanResponse>> getActivePlans() {
        return ResponseEntity.ok(adminPlanService.getActivePlans());
    }

    @Operation(summary = "Update Plan", description = "Updates an existing subscription plan.")
    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable UUID id, @RequestBody PlanRequest request) {
        return ResponseEntity.ok(adminPlanService.updatePlan(id, request));
    }

    @Operation(summary = "Toggle Plan Status", description = "Activates or deactivates a subscription plan.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> togglePlanStatus(@PathVariable UUID id) {
        adminPlanService.togglePlanStatus(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Plan status toggled successfully"));
    }
}
