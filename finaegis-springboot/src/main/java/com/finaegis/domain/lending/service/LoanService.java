package com.finaegis.domain.lending.service;

import com.finaegis.domain.lending.model.Loan;
import com.finaegis.domain.lending.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;

    public Loan applyForLoan(String borrowerId, BigDecimal principal,
                             BigDecimal interestRate, int termMonths, String assetCode) {
        Loan loan = Loan.builder()
            .id(UUID.randomUUID().toString())
            .borrowerId(borrowerId)
            .principal(principal)
            .interestRate(interestRate)
            .termMonths(termMonths)
            .assetCode(assetCode)
            .status(Loan.LoanStatus.PENDING)
            .riskScore(assessRisk(borrowerId, principal))
            .build();
        loan.setMonthlyPayment(calculateMonthlyPayment(principal, interestRate, termMonths));
        return loanRepository.save(loan);
    }

    public Loan approveLoan(String loanId, String lenderId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not pending");
        }
        loan.setLenderId(lenderId);
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    public Loan getLoan(String loanId) {
        return loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
    }

    public List<Loan> getLoansByBorrower(String borrowerId) {
        return loanRepository.findByBorrowerId(borrowerId);
    }

    /**
     * Amortized monthly payment using standard formula:
     * P * r * (1+r)^n / ((1+r)^n - 1)
     */
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

    /**
     * Simplified risk scoring: 0-100 (higher = riskier)
     */
    private int assessRisk(String borrowerId, BigDecimal principal) {
        int score = 40; // base
        if (principal.compareTo(BigDecimal.valueOf(10000)) > 0) {
            score += 20;
        }
        if (principal.compareTo(BigDecimal.valueOf(50000)) > 0) {
            score += 15;
        }
        return Math.min(score, 100);
    }
}
