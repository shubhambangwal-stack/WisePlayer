package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.CreditTransaction;
import com.iptv.wiseplayer.domain.enums.CreditTransactionType;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.CreditTransactionRepository;
import com.iptv.wiseplayer.repository.PlanConfigRepository;
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
    private final PlanConfigRepository planConfigRepository;

    @Override
    @Transactional
    public void deductCredits(UUID resellerId, String planName, UUID requestId) {
        BigDecimal cost = getActivationCost(planName);
        Admin reseller = adminRepository.findById(resellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        if (reseller.getCredits().compareTo(cost) < 0) {
            throw new BadRequestException(
                    "Insufficient credits. Required: " + cost + ", Available: " + reseller.getCredits());
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
        java.util.List<CreditTransaction> transactions = creditTransactionRepository
                .findAllByAdminIdOrderByCreatedAtDesc(resellerId);

        BigDecimal refundAmount = transactions.stream()
                .filter(t -> requestId.equals(t.getRelatedRequestId())
                        && t.getType() == CreditTransactionType.DEDUCTION)
                .findFirst()
                .map(t -> t.getAmount().abs())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Original deduction transaction not found for request: " + requestId));

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

        CreditTransaction transaction = new CreditTransaction();
        transaction.setAdminId(resellerId);
        transaction.setAmount(creditAmount);
        transaction.setType(CreditTransactionType.PURCHASE);

        String notes = "Credit purchase. Payment ID: " + paymentId;
        if (amount >= 1000) {
            int bonus = 200;
            BigDecimal bonusAmount = BigDecimal.valueOf(bonus);
            reseller.setCredits(reseller.getCredits().add(bonusAmount));
            notes += " (+ " + bonus + " free bonus codes)";
            transaction.setAmount(creditAmount.add(bonusAmount));
            log.info("Applied {} bonus credits for bulk purchase of {} codes to reseller {}", bonus, amount,
                    resellerId);
        }

        adminRepository.save(reseller);
        transaction.setNotes(notes);
        creditTransactionRepository.save(transaction);

        log.info("Added {} credits to reseller {} via payment {}", amount, resellerId, paymentId);
    }

    @Override
    public BigDecimal calculateUnitPrice(int quantity) {
        if (quantity >= 1000) {
            return new BigDecimal("1.00");
        }
        if (quantity >= 500)
            return new BigDecimal("1.25");
        if (quantity >= 200)
            return new BigDecimal("1.50");
        if (quantity >= 100)
            return new BigDecimal("1.75");
        if (quantity >= 50)
            return new BigDecimal("2.00");
        if (quantity > 10)
            return new BigDecimal("2.20");
        if (quantity >= 1)
            return new BigDecimal("2.50");
        return BigDecimal.ZERO;
    }

    @Override
    public java.math.BigDecimal getActivationCost(String planName) {
        if (planName == null || planName.trim().isEmpty()) {
            throw new BadRequestException("Plan name must be provided");
        }

        return planConfigRepository.findByName(planName)
                .map(plan -> {
                    if (!plan.isActive()) {
                        throw new BadRequestException("The selected plan [" + planName + "] is currently inactive.");
                    }
                    return plan.getPrice();
                })
                .orElseThrow(() -> new ResourceNotFoundException("Unrecognized plan name: " + planName));
    }

    @Override
    public BigDecimal getBalance(UUID resellerId) {
        return adminRepository.findById(resellerId)
                .map(Admin::getCredits)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public org.springframework.data.domain.Page<com.iptv.wiseplayer.dto.response.CreditTransactionResponse> getTransactionHistory(
            UUID resellerId, org.springframework.data.domain.Pageable pageable) {
        return creditTransactionRepository.findAllByAdminIdOrderByCreatedAtDesc(resellerId, pageable)
                .map(transaction -> {
                    com.iptv.wiseplayer.dto.response.CreditTransactionResponse response = new com.iptv.wiseplayer.dto.response.CreditTransactionResponse();
                    response.setId(transaction.getId());
                    response.setAmount(transaction.getAmount());
                    response.setType(transaction.getType());
                    response.setNotes(transaction.getNotes());
                    response.setRelatedRequestId(transaction.getRelatedRequestId());
                    response.setCreatedAt(transaction.getCreatedAt());
                    return response;
                });
    }

    @Override
    @Transactional
    public void transferCredits(UUID fromId, UUID toId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transfer amount must be greater than zero");
        }

        Admin sender = adminRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender reseller not found"));

        if (sender.getCredits().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient credits for transfer. Available: " + sender.getCredits());
        }

        Admin receiver = adminRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver reseller not found"));

        // Deduct from sender
        sender.setCredits(sender.getCredits().subtract(amount));
        adminRepository.save(sender);

        CreditTransaction outTx = new CreditTransaction();
        outTx.setAdminId(fromId);
        outTx.setAmount(amount.negate());
        outTx.setType(CreditTransactionType.TRANSFER_OUT);
        outTx.setNotes("Credit transfer to sub-reseller: " + receiver.getUsername());
        creditTransactionRepository.save(outTx);

        // Add to receiver
        receiver.setCredits(receiver.getCredits().add(amount));
        adminRepository.save(receiver);

        CreditTransaction inTx = new CreditTransaction();
        inTx.setAdminId(toId);
        inTx.setAmount(amount);
        inTx.setType(CreditTransactionType.TRANSFER_IN);
        inTx.setNotes("Credit transfer from parent reseller: " + sender.getUsername());
        creditTransactionRepository.save(inTx);

        log.info("Transferred {} credits from {} to {}", amount, fromId, toId);
    }
}
