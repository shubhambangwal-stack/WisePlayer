package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.ResellerCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResellerCustomerRepository extends JpaRepository<ResellerCustomer, UUID> {
    Optional<ResellerCustomer> findByMacAddress(String macAddress);
    Optional<ResellerCustomer> findByResellerIdAndMacAddress(UUID resellerId, String macAddress);
}
