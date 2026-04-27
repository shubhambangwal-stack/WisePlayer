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

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Admin a WHERE a.role IN :roles AND (" +
           "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:email IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:fullName IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))))")
    Page<Admin> searchResellersWithFilters(java.util.List<AdminRole> roles, String username, String email, String fullName, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Admin a WHERE a.parentId = :parentId AND (" +
           "(:search IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(:search IS NULL OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Admin> searchSubResellers(UUID parentId, String search, Pageable pageable);

    Page<Admin> findAllByRoleIn(List<AdminRole> roles, Pageable pageable);

    List<Admin> findAllByRoleIn(List<AdminRole> roles);
}
