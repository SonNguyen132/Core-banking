package com.finaegis.domain.payment.event;

import lombok.Value;
import java.time.Instant;

@Value
public class TransferFailedEvent {
    String transferId;
    String reason;
    Instant failedAt;
}
