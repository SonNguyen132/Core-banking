package com.finaegis.domain.exchange.event;

import lombok.Value;
import java.time.Instant;
import java.math.BigDecimal;

@Value
public class OrderCancelledEvent {
    String orderId;
    String reason;
    Instant cancelledAt;
}
