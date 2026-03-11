package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByFullName(String fullName);
}
