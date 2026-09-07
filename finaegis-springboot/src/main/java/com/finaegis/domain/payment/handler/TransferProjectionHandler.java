package com.finaegis.domain.payment.handler;

import com.finaegis.domain.payment.event.TransferCompletedEvent;
import com.finaegis.domain.payment.event.TransferFailedEvent;
import com.finaegis.domain.payment.event.TransferInitiatedEvent;
import com.finaegis.domain.payment.event.TransferRolledBackEvent;
import com.finaegis.domain.payment.model.TransferView;
import com.finaegis.domain.payment.repository.TransferViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferProjectionHandler {

    private final TransferViewRepository repository;

    @EventHandler
    public void on(TransferInitiatedEvent event) {
        TransferView view = TransferView.builder()
            .transferId(event.getTransferId())
            .fromAccountId(event.getFromAccountId())
            .toAccountId(event.getToAccountId())
            .amount(event.getAmount())
            .currency(event.getCurrency())
            .description(event.getDescription())
            .initiatedBy(event.getInitiatedBy())
            .status(TransferView.TransferStatus.PENDING)
            .build();
        repository.save(view);
        log.info("Transfer view created: {}", event.getTransferId());
    }

    @EventHandler
    public void on(TransferCompletedEvent event) {
        repository.findById(event.getTransferId()).ifPresent(view -> {
            view.setStatus(TransferView.TransferStatus.COMPLETED);
            repository.save(view);
            log.info("Transfer completed: {}", event.getTransferId());
        });
    }

    @EventHandler
    public void on(TransferFailedEvent event) {
        repository.findById(event.getTransferId()).ifPresent(view -> {
            view.setStatus(TransferView.TransferStatus.FAILED);
            repository.save(view);
            log.info("Transfer failed: {} - {}", event.getTransferId(), event.getReason());
        });
    }

    @EventHandler
    public void on(TransferRolledBackEvent event) {
        repository.findById(event.getTransferId()).ifPresent(view -> {
            view.setStatus(TransferView.TransferStatus.ROLLED_BACK);
            repository.save(view);
            log.info("Transfer rolled back: {}", event.getTransferId());
        });
    }
}
