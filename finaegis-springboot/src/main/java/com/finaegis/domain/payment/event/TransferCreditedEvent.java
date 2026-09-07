package com.finaegis.domain.payment.event;

import lombok.Value;
import java.math.BigDecimal;
import java.time.Instant;

@Value
public class TransferCreditedEvent {
    String transferId;
    String toAccountId;
    BigDecimal amount;
    Instant createdAt;
}
