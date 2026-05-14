package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, UUID> {
    Optional<SuperAdmin> findByUsername(String username);

    Optional<SuperAdmin> findByFullName(String fullName);
    Optional<SuperAdmin> findByEmail(String email);
    boolean existsByEmail(String email);
}
