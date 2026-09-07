package com.finaegis.domain.account.event;

import lombok.Value;
import java.time.Instant;

@Value
public class AccountCreatedEvent {
    String accountId;
    String name;
    String userId;
    String assetCode;
    Instant createdAt;
}
