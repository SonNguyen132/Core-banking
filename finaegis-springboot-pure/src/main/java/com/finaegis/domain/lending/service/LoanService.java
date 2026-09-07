package com.finaegis.domain.lending.service;

import com.finaegis.domain.lending.model.Loan;
import com.finaegis.domain.lending.model.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;

    public Loan apply(String borrowerId, BigDecimal principal, BigDecimal interestRate,
                      int termMonths, String assetCode) {
        Loan loan = Loan.builder()
            .id(UUID.randomUUID().toString())
            .borrowerId(borrowerId)
            .principal(principal)
            .interestRate(interestRate)
            .termMonths(termMonths)
            .assetCode(assetCode)
            .status(Loan.LoanStatus.PENDING)
            .monthlyPayment(calculateMonthlyPayment(principal, interestRate, termMonths))
            .build();
        return loanRepository.save(loan);
    }

    public Loan approve(String loanId, String lenderId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not pending");
        }
        loan.setLenderId(lenderId);
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    public List<Loan> byBorrower(String borrowerId) {
        return loanRepository.findByBorrowerId(borrowerId);
    }

    public BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 8, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(termMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 8, RoundingMode.HALF_UP);
    }
}
