package com.finaegis.domain.lending.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    List<Loan> findByBorrowerId(String borrowerId);

    List<Loan> findByStatus(Loan.LoanStatus status);
}
