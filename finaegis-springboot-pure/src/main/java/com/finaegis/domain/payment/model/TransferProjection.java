package com.finaegis.domain.payment.model;

import com.finaegis.domain.payment.event.TransferCompletedEvent;
import com.finaegis.domain.payment.event.TransferFailedEvent;
import com.finaegis.domain.payment.event.TransferInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Projection for transfers - keeps transfer_view read model in sync.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferProjection {

    private final TransferViewRepository repository;

    @EventListener
    public void on(TransferInitiatedEvent event) {
        TransferView view = TransferView.builder()
            .transferId(event.getAggregateId())
            .fromAccountId(event.fromAccountId)
            .toAccountId(event.toAccountId)
            .amount(event.amount)
            .currency(event.currency)
            .description(event.description)
            .initiatedBy(event.initiatedBy)
            .status("PENDING")
            .build();
        repository.save(view);
    }

    @EventListener
    public void on(TransferCompletedEvent event) {
        repository.findById(event.getAggregateId()).ifPresent(view -> {
            view.setStatus("COMPLETED");
            repository.save(view);
        });
    }

    @EventListener
    public void on(TransferFailedEvent event) {
        repository.findById(event.getAggregateId()).ifPresent(view -> {
            view.setStatus("FAILED");
            repository.save(view);
        });
    }
}
