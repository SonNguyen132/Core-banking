package com.finaegis.domain.account.event;

import com.finaegis.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
public class MoneyDepositedEvent extends DomainEvent {

    public BigDecimal amount;
    public String currency;
    public String reference;
    public Instant happenedAt;

    public MoneyDepositedEvent(BigDecimal amount, String currency, String reference) {
        this(amount, currency, reference, Instant.now());
    }
}
