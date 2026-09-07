package com.finaegis.domain.payment.event;

import lombok.Value;
import java.time.Instant;

@Value
public class TransferCompletedEvent {
    String transferId;
    Instant completedAt;
}
