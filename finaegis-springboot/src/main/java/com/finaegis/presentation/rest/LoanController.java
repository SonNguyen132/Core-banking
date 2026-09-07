package com.finaegis.presentation.rest;

import com.finaegis.domain.lending.model.Loan;
import com.finaegis.domain.lending.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<Loan> apply(@RequestBody LoanApplicationRequest request) {
        Loan loan = loanService.applyForLoan(
            request.borrowerId(),
            request.principal(),
            request.interestRate(),
            request.termMonths(),
            request.assetCode()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    @PostMapping("/{loanId}/approve")
    public ResponseEntity<Loan> approve(@PathVariable String loanId, @RequestBody ApproveRequest request) {
        return ResponseEntity.ok(loanService.approveLoan(loanId, request.lenderId()));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<Loan> get(@PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    @GetMapping("/borrower/{borrowerId}")
    public ResponseEntity<List<Loan>> byBorrower(@PathVariable String borrowerId) {
        return ResponseEntity.ok(loanService.getLoansByBorrower(borrowerId));
    }

    public record LoanApplicationRequest(String borrowerId, BigDecimal principal,
                                         BigDecimal interestRate, int termMonths, String assetCode) {}
    public record ApproveRequest(String lenderId) {}
}
