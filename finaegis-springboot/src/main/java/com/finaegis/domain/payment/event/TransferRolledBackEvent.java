package com.finaegis.domain.payment.event;

import lombok.Value;
import java.math.BigDecimal;
import java.time.Instant;

@Value
public class TransferRolledBackEvent {
    String transferId;
    String fromAccountId;
    BigDecimal amount;
    Instant rolledBackAt;
}
