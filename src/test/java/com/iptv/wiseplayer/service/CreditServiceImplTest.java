package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.CreditTransaction;
import com.iptv.wiseplayer.domain.entity.SubscriptionPlanConfig;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.domain.enums.CreditTransactionType;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.CreditTransactionRepository;
import com.iptv.wiseplayer.repository.PlanConfigRepository;
import com.iptv.wiseplayer.service.impl.CreditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreditServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private CreditTransactionRepository creditTransactionRepository;

    @Mock
    private PlanConfigRepository planConfigRepository;

    @InjectMocks
    private CreditServiceImpl creditService;

    private final UUID resellerId = UUID.randomUUID();
    private final UUID subResellerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addCredits_ShouldAddCreditsAndBonus_WhenAmountIs1000OrMore() {
        // Arrange
        Admin reseller = new Admin();
        reseller.setId(resellerId);
        reseller.setCredits(BigDecimal.ZERO);
        when(adminRepository.findById(resellerId)).thenReturn(Optional.of(reseller));

        // Act
        creditService.addCredits(resellerId, 1000, "PAY-123");

        // Assert
        // 1000 + 200 bonus = 1200
        assertEquals(new BigDecimal("1200"), reseller.getCredits());
        verify(adminRepository).save(reseller);
        verify(creditTransactionRepository).save(any(CreditTransaction.class));
    }

    @Test
    void deductCredits_ShouldDeductCredits_WhenBalanceIsSufficient() {
        // Arrange
        Admin reseller = new Admin();
        reseller.setId(resellerId);
        reseller.setCredits(new BigDecimal("100"));
        when(adminRepository.findById(resellerId)).thenReturn(Optional.of(reseller));

        SubscriptionPlanConfig plan = new SubscriptionPlanConfig();
        plan.setName("ANNUAL");
        plan.setCredits(BigDecimal.TEN);
        plan.setActive(true);
        when(planConfigRepository.findByName("ANNUAL")).thenReturn(Optional.of(plan));

        UUID requestId = UUID.randomUUID();

        // Act
        creditService.deductCredits(resellerId, "ANNUAL", requestId);

        // Assert
        assertEquals(new BigDecimal("90"), reseller.getCredits());
        verify(adminRepository).save(reseller);
        verify(creditTransactionRepository).save(any(CreditTransaction.class));
    }

    @Test
    void deductCredits_ShouldThrowException_WhenBalanceIsInsufficient() {
        // Arrange
        Admin reseller = new Admin();
        reseller.setId(resellerId);
        reseller.setCredits(BigDecimal.ONE);
        when(adminRepository.findById(resellerId)).thenReturn(Optional.of(reseller));

        SubscriptionPlanConfig plan = new SubscriptionPlanConfig();
        plan.setName("ANNUAL");
        plan.setCredits(BigDecimal.TEN);
        plan.setActive(true);
        when(planConfigRepository.findByName("ANNUAL")).thenReturn(Optional.of(plan));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
            creditService.deductCredits(resellerId, "ANNUAL", UUID.randomUUID()));
    }

    @Test
    void transferCredits_ShouldTransferSuccessfully_WhenValid() {
        // Arrange
        Admin sender = new Admin();
        sender.setId(resellerId);
        sender.setRole(AdminRole.RESELLER);
        sender.setCredits(new BigDecimal("50"));
        sender.setActive(true);

        Admin receiver = new Admin();
        receiver.setId(subResellerId);
        receiver.setRole(AdminRole.SUB_RESELLER);
        receiver.setParentId(resellerId);
        receiver.setCredits(BigDecimal.ZERO);
        receiver.setActive(true);

        when(adminRepository.findById(resellerId)).thenReturn(Optional.of(sender));
        when(adminRepository.findById(subResellerId)).thenReturn(Optional.of(receiver));

        // Act
        creditService.transferCredits(resellerId, subResellerId, new BigDecimal("20"));

        // Assert
        assertEquals(new BigDecimal("30"), sender.getCredits());
        assertEquals(new BigDecimal("20"), receiver.getCredits());
        verify(adminRepository, times(2)).save(any(Admin.class));
        verify(creditTransactionRepository, times(2)).save(any(CreditTransaction.class));
    }

    @Test
    void calculateUnitPrice_ShouldReturnCorrectTiers() {
        assertEquals(new BigDecimal("2.50"), creditService.calculateUnitPrice(1));
        assertEquals(new BigDecimal("2.20"), creditService.calculateUnitPrice(20));
        assertEquals(new BigDecimal("2.00"), creditService.calculateUnitPrice(50));
        assertEquals(new BigDecimal("1.75"), creditService.calculateUnitPrice(100));
        assertEquals(new BigDecimal("1.50"), creditService.calculateUnitPrice(200));
        assertEquals(new BigDecimal("1.25"), creditService.calculateUnitPrice(500));
        assertEquals(new BigDecimal("1.00"), creditService.calculateUnitPrice(1000));
    }
}
