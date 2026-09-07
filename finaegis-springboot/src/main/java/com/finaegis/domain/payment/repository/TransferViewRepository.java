package com.finaegis.domain.payment.repository;

import com.finaegis.domain.payment.model.TransferView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferViewRepository extends JpaRepository<TransferView, String> {

    List<TransferView> findByFromAccountId(String fromAccountId);

    List<TransferView> findByToAccountId(String toAccountId);

    List<TransferView> findByStatus(TransferView.TransferStatus status);
}
