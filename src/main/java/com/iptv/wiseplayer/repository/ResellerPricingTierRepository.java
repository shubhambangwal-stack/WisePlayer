package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.ResellerPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ResellerPricingTierRepository extends JpaRepository<ResellerPricingTier, UUID> {

    @Query("SELECT r FROM ResellerPricingTier r WHERE r.minQuantity <= :quantity " +
           "AND (r.maxQuantity IS NULL OR r.maxQuantity >= :quantity) " +
           "ORDER BY r.minQuantity DESC LIMIT 1")
    Optional<ResellerPricingTier> findApplicableTier(@Param("quantity") int quantity);

    List<ResellerPricingTier> findAllByOrderByMinQuantityAsc();
}
