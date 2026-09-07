package com.finaegis.domain.exchange.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.Value;

@Value
public class CancelOrderCommand {
    @TargetAggregateIdentifier
    String orderId;
    String reason;
}
