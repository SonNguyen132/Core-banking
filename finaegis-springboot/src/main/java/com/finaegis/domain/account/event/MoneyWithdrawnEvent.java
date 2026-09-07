package com.finaegis.domain.account.event;

import lombok.Value;
import java.math.BigDecimal;
import java.time.Instant;

@Value
public class MoneyWithdrawnEvent {
    String accountId;
    BigDecimal amount;
    String currency;
    String reference;
    Instant createdAt;
}
