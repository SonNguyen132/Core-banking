package com.finaegis.domain.exchange.event;

import lombok.Value;
import java.math.BigDecimal;
import java.time.Instant;

@Value
public class OrderMatchedEvent {
    String buyOrderId;
    String sellOrderId;
    BigDecimal quantity;
    BigDecimal price;
    String symbol;
    Instant matchedAt;
}
