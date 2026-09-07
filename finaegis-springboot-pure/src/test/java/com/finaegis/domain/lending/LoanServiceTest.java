package com.finaegis.domain.lending;

import com.finaegis.domain.lending.model.Loan;
import com.finaegis.domain.lending.model.LoanRepository;
import com.finaegis.domain.lending.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository);
    }

    @Test
    void shouldCalculateMonthlyPayment() {
        BigDecimal payment = loanService.calculateMonthlyPayment(
            new BigDecimal("1000.00"),
            new BigDecimal("12.00"),
            12);
        assertNotNull(payment);
        assertTrue(payment.compareTo(new BigDecimal("88.00")) > 0);
        assertTrue(payment.compareTo(new BigDecimal("90.00")) < 0);
    }

    @Test
    void shouldCalculateMonthlyPaymentForZeroInterest() {
        BigDecimal payment = loanService.calculateMonthlyPayment(
            new BigDecimal("1200.00"),
            BigDecimal.ZERO,
            12);
        assertEquals(0, payment.compareTo(new BigDecimal("100.00000000")));
    }

    @Test
    void shouldApplyForLoan() {
        when(loanRepository.save(any(Loan.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Loan loan = loanService.apply(
            "borrower-1",
            new BigDecimal("5000.00"),
            new BigDecimal("10.00"),
            24,
            "USD");

        assertNotNull(loan.getId());
        assertEquals(Loan.LoanStatus.PENDING, loan.getStatus());
        assertNotNull(loan.getMonthlyPayment());
    }

    @Test
    void shouldApproveLoan() {
        Loan pending = Loan.builder()
            .id("loan-1")
            .borrowerId("borrower-1")
            .principal(new BigDecimal("5000.00"))
            .interestRate(new BigDecimal("10.00"))
            .termMonths(24)
            .assetCode("USD")
            .status(Loan.LoanStatus.PENDING)
            .build();
        when(loanRepository.findById("loan-1")).thenReturn(java.util.Optional.of(pending));
        when(loanRepository.save(any(Loan.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Loan approved = loanService.approve("loan-1", "lender-1");

        assertEquals(Loan.LoanStatus.ACTIVE, approved.getStatus());
        assertEquals("lender-1", approved.getLenderId());
    }

    @Test
    void shouldNotApproveNonPendingLoan() {
        Loan active = Loan.builder()
            .id("loan-1")
            .borrowerId("borrower-1")
            .status(Loan.LoanStatus.ACTIVE)
            .build();
        when(loanRepository.findById("loan-1")).thenReturn(java.util.Optional.of(active));

        assertThrows(IllegalStateException.class,
            () -> loanService.approve("loan-1", "lender-1"));
    }
}
