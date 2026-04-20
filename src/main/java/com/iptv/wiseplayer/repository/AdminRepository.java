package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByUsername(String username);

    List<Admin> findAllByParentId(UUID parentId);

    Page<Admin> findAllByParentId(UUID parentId, Pageable pageable);

    Page<Admin> findAllByRoleIn(List<AdminRole> roles, Pageable pageable);

    List<Admin> findAllByRoleIn(List<AdminRole> roles);
    org.springframework.data.domain.Page<Admin> findAllByParentId(java.util.UUID parentId, org.springframework.data.domain.Pageable pageable);
}
