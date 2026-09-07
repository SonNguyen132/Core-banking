package com.finaegis.domain.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.Value;
import java.math.BigDecimal;

@Value
public class DepositMoneyCommand {
    @TargetAggregateIdentifier
    String accountId;
    BigDecimal amount;
    String currency;
    String reference;
}
