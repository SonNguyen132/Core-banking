package com.finaegis.domain.account.command;

import lombok.Value;
import java.math.BigDecimal;

@Value
public class CreateAccountCommand {
    String accountId;
    String name;
    String userId;
    String assetCode;
}
