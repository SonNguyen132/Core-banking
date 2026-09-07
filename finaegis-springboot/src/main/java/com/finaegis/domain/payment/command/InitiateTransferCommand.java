package com.finaegis.domain.payment.command;

import lombok.Value;
import java.math.BigDecimal;

@Value
public class InitiateTransferCommand {
    String transferId;
    String fromAccountId;
    String toAccountId;
    BigDecimal amount;
    String currency;
    String description;
    String initiatedBy;
}
