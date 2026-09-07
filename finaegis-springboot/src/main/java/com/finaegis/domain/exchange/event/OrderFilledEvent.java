package com.finaegis.domain.exchange.event;

import lombok.Value;
import java.math.BigDecimal;

@Value
public class OrderFilledEvent {
    String orderId;
    BigDecimal filledQuantity;
    BigDecimal averagePrice;
}
