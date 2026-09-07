package com.finaegis.presentation.rest;

import com.finaegis.domain.lending.model.Loan;
import com.finaegis.domain.lending.service.LoanService;
import com.finaegis.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('" + PermissionConstants.LOAN_APPLY + "')")
    public ResponseEntity<Loan> apply(@RequestBody LoanApplicationRequest request) {
        Loan loan = loanService.apply(
            request.borrowerId(),
            request.principal(),
            request.interestRate(),
            request.termMonths(),
            request.assetCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasAuthority('" + PermissionConstants.LOAN_APPROVE + "')")
    public ResponseEntity<Loan> approve(@PathVariable String loanId, @RequestBody ApproveRequest request) {
        return ResponseEntity.ok(loanService.approve(loanId, request.lenderId()));
    }

    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.LOAN_VIEW + "')")
    public ResponseEntity<List<Loan>> byBorrower(@PathVariable String borrowerId) {
        return ResponseEntity.ok(loanService.byBorrower(borrowerId));
    }

    public record LoanApplicationRequest(String borrowerId, BigDecimal principal,
                                         BigDecimal interestRate, int termMonths, String assetCode) {}
    public record ApproveRequest(String lenderId) {}
}
