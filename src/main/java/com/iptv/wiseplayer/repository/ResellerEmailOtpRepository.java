package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.ResellerEmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResellerEmailOtpRepository extends JpaRepository<ResellerEmailOtp, UUID> {
    Optional<ResellerEmailOtp> findByAdminId(UUID adminId);
    void deleteByAdminId(UUID adminId);
}