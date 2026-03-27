package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.CreditTransaction;
import com.iptv.wiseplayer.domain.enums.CreditTransactionType;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.CreditTransactionRepository;
import com.iptv.wiseplayer.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditServiceImpl implements CreditService {

    private final AdminRepository adminRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    @Override
    @Transactional
    public void deductCredits(UUID resellerId, String planName, UUID requestId) {
        BigDecimal cost = getActivationCost(planName);
        Admin reseller = adminRepository.findById(resellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        if (reseller.getCredits().compareTo(cost) < 0) {
            throw new BadRequestException("Insufficient credits. Required: " + cost + ", Available: " + reseller.getCredits());
        }

        reseller.setCredits(reseller.getCredits().subtract(cost));
        adminRepository.save(reseller);

        CreditTransaction transaction = new CreditTransaction();
        transaction.setAdminId(resellerId);
        transaction.setAmount(cost.negate());
        transaction.setType(CreditTransactionType.DEDUCTION);
        transaction.setRelatedRequestId(requestId);
        transaction.setNotes("Activation request for " + planName);
        creditTransactionRepository.save(transaction);

        log.info("Deducted {} credits from reseller {} for request {}", cost, resellerId, requestId);
    }

    @Override
    @Transactional
    public void refundCredits(UUID resellerId, UUID requestId) {
        // Find the original deduction transaction to get the exact amount
        java.util.List<CreditTransaction> transactions = creditTransactionRepository.findAllByAdminIdOrderByCreatedAtDesc(resellerId);
        
        BigDecimal refundAmount = transactions.stream()
                .filter(t -> requestId.equals(t.getRelatedRequestId()) && t.getType() == CreditTransactionType.DEDUCTION)
                .findFirst()
                .map(t -> t.getAmount().abs())
                .orElseThrow(() -> new ResourceNotFoundException("Original deduction transaction not found for request: " + requestId));

        Admin reseller = adminRepository.findById(resellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        reseller.setCredits(reseller.getCredits().add(refundAmount));
        adminRepository.save(reseller);

        CreditTransaction transaction = new CreditTransaction();
        transaction.setAdminId(resellerId);
        transaction.setAmount(refundAmount);
        transaction.setType(CreditTransactionType.REFUND);
        transaction.setRelatedRequestId(requestId);
        transaction.setNotes("Refund for rejected/cancelled request " + requestId);
        creditTransactionRepository.save(transaction);

        log.info("Refunded {} credits to reseller {} for request {}", refundAmount, resellerId, requestId);
    }

    @Override
    @Transactional
    public void addCredits(UUID resellerId, int amount, String paymentId) {
        BigDecimal creditAmount = BigDecimal.valueOf(amount);
        Admin reseller = adminRepository.findById(resellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        reseller.setCredits(reseller.getCredits().add(creditAmount));
        adminRepository.save(reseller);

        CreditTransaction transaction = new CreditTransaction();
        transaction.setAdminId(resellerId);
        transaction.setAmount(creditAmount);
        transaction.setType(CreditTransactionType.PURCHASE);
        transaction.setNotes("Credit purchase. Payment ID: " + paymentId);
        creditTransactionRepository.save(transaction);

        log.info("Added {} credits to reseller {} via payment {}", amount, resellerId, paymentId);
    }

    @Override
    public BigDecimal calculateUnitPrice(int quantity) {
        if (quantity >= 1000) return new BigDecimal("1.49");
        if (quantity >= 500) return new BigDecimal("2.49");
        if (quantity >= 200) return new BigDecimal("3.49");
        if (quantity >= 100) return new BigDecimal("3.99");
        if (quantity >= 50) return new BigDecimal("4.99");
        if (quantity >= 10) return new BigDecimal("5.49");
        if (quantity >= 1) return new BigDecimal("5.99");
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getActivationCost(String planName) {
        if (planName == null) return BigDecimal.ZERO;
        String upperPlan = planName.toUpperCase();
        if (upperPlan.contains("LIFETIME") || upperPlan.contains("FOREVER")) {
            return new BigDecimal("2.5");
        }
        if (upperPlan.contains("ANNUAL") || upperPlan.contains("YEAR")) {
            return new BigDecimal("1.0");
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getBalance(UUID resellerId) {
        return adminRepository.findById(resellerId)
                .map(Admin::getCredits)
                .orElse(BigDecimal.ZERO);
    }
}
