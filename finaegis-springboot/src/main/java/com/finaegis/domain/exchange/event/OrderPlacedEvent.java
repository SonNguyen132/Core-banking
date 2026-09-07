package com.finaegis.domain.exchange.event;

import lombok.Value;
import java.time.Instant;
import java.math.BigDecimal;

@Value
public class OrderPlacedEvent {
    String orderId;
    String accountId;
    String symbol;
    String side;
    String type;
    BigDecimal quantity;
    BigDecimal price;
    Instant createdAt;
}
