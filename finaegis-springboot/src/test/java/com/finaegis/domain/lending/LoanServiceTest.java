package com.finaegis.domain.lending;

import com.finaegis.domain.lending.model.Loan;
import com.finaegis.domain.lending.repository.LoanRepository;
import com.finaegis.domain.lending.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        // 1000 @ 12% annual, 12 months
        BigDecimal payment = loanService.calculateMonthlyPayment(
            new BigDecimal("1000.00"),
            new BigDecimal("12.00"),
            12);
        // Monthly payment ~ 88.85
        assertNotNull(payment);
        assertEquals(0, payment.compareTo(new BigDecimal("88.8487")));
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

        Loan loan = loanService.applyForLoan(
            "borrower-1",
            new BigDecimal("5000.00"),
            new BigDecimal("10.00"),
            24,
            "USD"
        );

        assertNotNull(loan.getId());
        assertEquals(Loan.LoanStatus.PENDING, loan.getStatus());
        assertNotNull(loan.getMonthlyPayment());
    }
}
