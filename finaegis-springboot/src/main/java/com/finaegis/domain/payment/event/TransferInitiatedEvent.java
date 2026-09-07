package com.finaegis.domain.payment.event;

import lombok.Value;
import java.math.BigDecimal;
import java.time.Instant;

@Value
public class TransferInitiatedEvent {
    String transferId;
    String fromAccountId;
    String toAccountId;
    BigDecimal amount;
    String currency;
    String description;
    String initiatedBy;
    Instant createdAt;
}
