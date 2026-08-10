package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.ResellerEmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResellerEmailOtpRepository extends JpaRepository<ResellerEmailOtp, UUID> {
    Optional<ResellerEmailOtp> findByAdminId(UUID adminId);
    void deleteByAdminId(UUID adminId);
    void deleteByAdminIdAndExpiresAtBefore(UUID adminId, java.time.LocalDateTime now);
    @Modifying
    @Query("DELETE FROM ResellerEmailOtp o WHERE o.expiresAt < :now")
    void deleteAllExpired(@Param("now") java.time.LocalDateTime now);
}