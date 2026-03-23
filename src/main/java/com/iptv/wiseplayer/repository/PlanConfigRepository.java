package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.SubscriptionPlanConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanConfigRepository extends JpaRepository<SubscriptionPlanConfig, UUID> {
    Optional<SubscriptionPlanConfig> findByName(String name);
    List<SubscriptionPlanConfig> findAllByActiveTrue();
}
