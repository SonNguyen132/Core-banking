package com.finaegis.domain.account.event;

import lombok.Value;
import java.time.Instant;

@Value
public class AccountClosedEvent {
    String accountId;
    String reason;
    Instant createdAt;
}
