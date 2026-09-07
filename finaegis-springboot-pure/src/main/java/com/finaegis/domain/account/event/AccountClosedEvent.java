package com.finaegis.domain.account.event;

import com.finaegis.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
public class AccountClosedEvent extends DomainEvent {

    public String reason;
    public Instant happenedAt;

    public AccountClosedEvent(String reason) {
        this(reason, Instant.now());
    }
}
