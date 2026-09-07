package com.finaegis.domain.payment.aggregate;

import com.finaegis.domain.payment.event.TransferCompletedEvent;
import com.finaegis.domain.payment.event.TransferFailedEvent;
import com.finaegis.domain.payment.event.TransferInitiatedEvent;
import com.finaegis.eventstore.AggregateRoot;

import java.math.BigDecimal;

/**
 * Transfer aggregate - pure event sourcing.
 * Tracks the lifecycle of a single money transfer.
 */
public class Transfer extends AggregateRoot {

    public static final String TYPE = "com.finaegis.domain.payment";

    public enum Status {
        PENDING, COMPLETED, FAILED
    }

    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String initiatedBy;
    private Status status;
    private String failureReason;

    public Transfer() {
        super(TYPE);
    }

    public void initiate(String fromAccountId, String toAccountId, BigDecimal amount,
                         String currency, String description, String initiatedBy) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        apply(new TransferInitiatedEvent(
            fromAccountId, toAccountId, amount, currency, description, initiatedBy));
    }

    public void complete() {
        if (status != Status.PENDING) {
            return;
        }
        apply(new TransferCompletedEvent());
    }

    public void fail(String reason) {
        if (status == Status.FAILED || status == Status.COMPLETED) {
            return;
        }
        apply(new TransferFailedEvent(reason));
    }

    // ---- apply handlers (replay state) ----

    void on(TransferInitiatedEvent event) {
        this.fromAccountId = event.fromAccountId;
        this.toAccountId = event.toAccountId;
        this.amount = event.amount;
        this.currency = event.currency;
        this.description = event.description;
        this.initiatedBy = event.initiatedBy;
        this.status = Status.PENDING;
    }

    void on(TransferCompletedEvent event) {
        this.status = Status.COMPLETED;
    }

    void on(TransferFailedEvent event) {
        this.status = Status.FAILED;
        this.failureReason = event.reason;
    }

    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public String getInitiatedBy() { return initiatedBy; }
    public Status getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
}
