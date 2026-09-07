package com.finaegis.domain.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.Value;

@Value
public class UnfreezeAccountCommand {
    @TargetAggregateIdentifier
    String accountId;
}
