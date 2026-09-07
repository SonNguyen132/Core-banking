package com.finaegis.domain.payment.event;

import com.finaegis.domain.common.DomainEvent;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class TransferCompletedEvent extends DomainEvent {

    public Instant completedAt;

    public TransferCompletedEvent() {
        this.completedAt = Instant.now();
    }
}
