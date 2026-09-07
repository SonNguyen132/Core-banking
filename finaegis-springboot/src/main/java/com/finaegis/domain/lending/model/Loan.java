package com.finaegis.domain.lending.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    public enum LoanStatus {
        PENDING, APPROVED, ACTIVE, REPAYING, COMPLETED, DEFAULTED, CANCELLED
    }

    @Id
    private String id;

    @Column(name = "borrower_id", nullable = false)
    private String borrowerId;

    @Column(name = "lender_id")
    private String lenderId;

    @Column(name = "principal", nullable = false, precision = 20, scale = 8)
    private BigDecimal principal;

    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate; // annual percentage

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "asset_code", nullable = false, length = 10)
    private String assetCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "monthly_payment", precision = 20, scale = 8)
    private BigDecimal monthlyPayment;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
