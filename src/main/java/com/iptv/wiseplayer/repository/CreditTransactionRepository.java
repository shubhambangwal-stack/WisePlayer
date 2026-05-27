package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    List<CreditTransaction> findAllByAdminIdOrderByCreatedAtDesc(UUID adminId);

    org.springframework.data.domain.Page<CreditTransaction> findAllByAdminIdOrderByCreatedAtDesc(UUID adminId,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM CreditTransaction t WHERE t.adminId = :adminId AND " +
           "(:search IS NULL OR :search = '' OR " +
           "CAST(t.id AS string) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(t.amount AS string) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CAST(t.type AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.notes) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(t.createdAt AS string) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.createdAt DESC")
    org.springframework.data.domain.Page<CreditTransaction> searchTransactions(
            @org.springframework.data.repository.query.Param("adminId") UUID adminId,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable);
}
