package com.finaegis.domain.payment.aggregate;

import com.finaegis.domain.payment.command.CompleteTransferCommand;
import com.finaegis.domain.payment.command.InitiateTransferCommand;
import com.finaegis.domain.payment.event.TransferCompletedEvent;
import com.finaegis.domain.payment.event.TransferFailedEvent;
import com.finaegis.domain.payment.event.TransferInitiatedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
public class TransferAggregate {

    public enum TransferStatus {
        INITIATED, PENDING, COMPLETED, FAILED, ROLLED_BACK
    }

    @AggregateIdentifier
    private String transferId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private TransferStatus status;

    protected TransferAggregate() {
    }

    @CommandHandler
    public TransferAggregate(InitiateTransferCommand command) {
        if (command.getFromAccountId().equals(command.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        AggregateLifecycle.apply(new TransferInitiatedEvent(
            command.getTransferId(),
            command.getFromAccountId(),
            command.getToAccountId(),
            command.getAmount(),
            command.getCurrency(),
            command.getDescription(),
            command.getInitiatedBy(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(CompleteTransferCommand command) {
        if (this.status == TransferStatus.COMPLETED) {
            return;
        }
        AggregateLifecycle.apply(new TransferCompletedEvent(
            command.getTransferId(),
            Instant.now()
        ));
    }

    @EventSourcingHandler
    public void on(TransferInitiatedEvent event) {
        this.transferId = event.getTransferId();
        this.fromAccountId = event.getFromAccountId();
        this.toAccountId = event.getToAccountId();
        this.amount = event.getAmount();
        this.currency = event.getCurrency();
        this.status = TransferStatus.PENDING;
    }

    @EventSourcingHandler
    public void on(TransferCompletedEvent event) {
        this.status = TransferStatus.COMPLETED;
    }

    @EventSourcingHandler
    public void on(TransferFailedEvent event) {
        this.status = TransferStatus.FAILED;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }
}
