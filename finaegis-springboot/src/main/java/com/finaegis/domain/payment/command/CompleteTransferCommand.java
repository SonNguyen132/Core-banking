package com.finaegis.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.Value;

@Value
public class CompleteTransferCommand {
    @TargetAggregateIdentifier
    String transferId;
}
