package com.finaegis.domain.lending.repository;

import com.finaegis.domain.lending.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    List<Loan> findByBorrowerId(String borrowerId);

    List<Loan> findByLenderId(String lenderId);

    List<Loan> findByStatus(Loan.LoanStatus status);

    long countByStatus(Loan.LoanStatus status);
}
