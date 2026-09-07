package com.finaegis.domain.payment.model;

import com.finaegis.domain.payment.aggregate.Transfer;
import com.finaegis.eventstore.AggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Application service for transfer orchestration.
 * Initiate -> the recorded TransferInitiatedEvent is published,
 * and the TransferSaga reacts to run the multi-step flow.
 */
@Service
@RequiredArgsConstructor
public class TransferApplicationService {

    private final AggregateRepository aggregateRepository;
    private final TransferViewRepository viewRepository;

    public String initiate(String fromAccountId, String toAccountId, java.math.BigDecimal amount,
                           String currency, String description, String initiatedBy) {
        String transferId = UUID.randomUUID().toString();
        Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new);
        transfer.initiate(fromAccountId, toAccountId, amount, currency, description, initiatedBy);
        aggregateRepository.save(transfer, transfer.getVersion());
        return transferId;
    }

    public TransferView get(String transferId) {
        return viewRepository.findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
    }

    public List<TransferView> listByAccount(String accountId) {
        List<TransferView> from = viewRepository.findByFromAccountId(accountId);
        List<TransferView> to = viewRepository.findByToAccountId(accountId);
        from.addAll(to);
        return from;
    }
}
