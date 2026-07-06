package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.SubscriptionPlanConfig;
import com.iptv.wiseplayer.dto.request.PlanRequest;
import com.iptv.wiseplayer.dto.response.PlanResponse;
import com.iptv.wiseplayer.exception.ResourceAlreadyExistsException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.PlanConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminPlanService {

    private final PlanConfigRepository planConfigRepository;

    public AdminPlanService(PlanConfigRepository planConfigRepository) {
        this.planConfigRepository = planConfigRepository;
    }

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        if (planConfigRepository.findByName(request.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Plan name already exists");
        }

        SubscriptionPlanConfig plan = new SubscriptionPlanConfig();
        plan.setName(request.getName());
        plan.setDurationDays(request.getDurationDays());
        plan.setPrice(request.getPrice());
        plan.setCredits(request.getCredits());
        plan.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        plan.setDescription(request.getDescription());
        plan.setActive(true);

        return convertToResponse(planConfigRepository.save(plan));
    }

    public Page<PlanResponse> getAllPlans(Pageable pageable) {
        return planConfigRepository.findAll(pageable).map(this::convertToResponse);
    }

    public List<PlanResponse> getActivePlans() {
        return planConfigRepository.findAllByActiveTrue().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PlanResponse updatePlan(UUID id, PlanRequest request) {
        SubscriptionPlanConfig plan = planConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        plan.setName(request.getName());
        plan.setDurationDays(request.getDurationDays());
        plan.setPrice(request.getPrice());
        plan.setCredits(request.getCredits());
        plan.setCurrency(request.getCurrency());
        plan.setDescription(request.getDescription());

        return convertToResponse(planConfigRepository.save(plan));
    }

    @Transactional
    public void togglePlanStatus(UUID id) {
        SubscriptionPlanConfig plan = planConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        plan.setActive(!plan.isActive());
        planConfigRepository.save(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        SubscriptionPlanConfig plan = planConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        planConfigRepository.delete(plan);
    }

    private PlanResponse convertToResponse(SubscriptionPlanConfig plan) {
        PlanResponse response = new PlanResponse();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setDurationDays(plan.getDurationDays());
        response.setPrice(plan.getPrice());
        response.setCredits(plan.getCredits());
        response.setCurrency(plan.getCurrency());
        response.setDescription(plan.getDescription());
        response.setActive(plan.isActive());
        response.setCreatedAt(plan.getCreatedAt());
        return response;
    }
}
