package com.finaegis.domain.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.Value;

@Value
public class CloseAccountCommand {
    @TargetAggregateIdentifier
    String accountId;
    String reason;
}
