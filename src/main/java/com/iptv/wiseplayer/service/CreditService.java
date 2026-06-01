package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.response.CreditTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreditService {

    /**
     * Deducts credits from a reseller for an activation request.
     */
    void deductCredits(UUID resellerId, String planName, UUID requestId);

    /**
     * Refunds credits to a reseller for a rejected/cancelled activation request.
     */
    void refundCredits(UUID resellerId, UUID requestId);

    /**
     * Adds credits to a reseller after a successful purchase.
     */
    void addCredits(UUID resellerId, int amount, String paymentId);

    /**
     * Calculates the unit price for a credit purchase based on quantity.
     */
    BigDecimal calculateUnitPrice(int quantity);

    /**
     * Calculates the total cost for a credit activation based on plan name.
     */
    BigDecimal getActivationCost(String planName);

    /**
     * Gets the current credit balance for a reseller.
     */
    BigDecimal getBalance(UUID resellerId);

    /**
     * Gets the transaction history for a reseller.
     */
    Page<CreditTransactionResponse> getTransactionHistory(
            UUID resellerId,
            String search,
            String type,
            java.time.LocalDate dateFrom,   // ← new
            java.time.LocalDate dateTo,     // ← new
            BigDecimal minAmount,           // ← new
            BigDecimal maxAmount,           // ← new
            Pageable pageable);


    /**
     * Transfers credits from one reseller to another (typically parent to child).
     */
    void transferCredits(UUID fromId, UUID toId, BigDecimal amount);
}
