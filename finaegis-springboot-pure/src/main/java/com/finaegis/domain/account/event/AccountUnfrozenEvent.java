package com.finaegis.domain.account.event;

import com.finaegis.domain.common.DomainEvent;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class AccountUnfrozenEvent extends DomainEvent {

    public Instant happenedAt;

    public AccountUnfrozenEvent() {
        this.happenedAt = Instant.now();
    }
}
