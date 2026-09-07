package com.finaegis.domain.payment.event;

import com.finaegis.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Records the fact that a transfer was initiated on the Transfer aggregate.
 */
@NoArgsConstructor
@AllArgsConstructor
public class TransferInitiatedEvent extends DomainEvent {

    public String fromAccountId;
    public String toAccountId;
    public BigDecimal amount;
    public String currency;
    public String description;
    public String initiatedBy;
}
