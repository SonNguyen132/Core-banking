package com.finaegis.domain.account.event;

import com.finaegis.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent extends DomainEvent {

    public String name;
    public String userId;
    public String assetCode;
    public Instant createdAt;

    public AccountCreatedEvent(String name, String userId, String assetCode) {
        this(name, userId, assetCode, Instant.now());
    }
}
