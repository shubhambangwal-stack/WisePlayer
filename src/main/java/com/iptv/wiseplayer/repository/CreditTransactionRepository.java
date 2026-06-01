package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    List<CreditTransaction> findAllByAdminIdOrderByCreatedAtDesc(UUID adminId);

    org.springframework.data.domain.Page<CreditTransaction> findAllByAdminIdOrderByCreatedAtDesc(UUID adminId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t FROM CreditTransaction t WHERE t.adminId = :adminId AND " +
            "(:type IS NULL OR :type = '' OR LOWER(CAST(t.type AS string)) = LOWER(:type)) AND " +
            "(:search IS NULL OR :search = '' OR (" +                          // ← ( added
            "CAST(t.id AS string) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "CAST(t.amount AS string) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(t.type AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "CAST(t.createdAt AS string) LIKE LOWER(CONCAT('%', :search, '%')))) AND " + // ← )) added
            "(:dateFrom IS NULL OR t.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR t.createdAt <= :dateTo) AND " +
            "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR t.amount <= :maxAmount) " +
            "ORDER BY t.createdAt DESC")
    Page<CreditTransaction> searchTransactions(
            @Param("adminId") UUID adminId,
            @Param("type") String type,
            @Param("search") String search,
            @Param("dateFrom") java.time.LocalDateTime dateFrom,
            @Param("dateTo") java.time.LocalDateTime dateTo,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
