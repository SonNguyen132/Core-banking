package com.finaegis.domain.exchange.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Value;
import java.math.BigDecimal;

@Value
public class PlaceOrderCommand {
    @NotBlank
    String orderId;
    @NotBlank
    String accountId;
    String symbol;      // e.g. BTC/USD
    @NotNull
    OrderSide side;     // BUY or SELL
    @NotNull
    OrderType type;     // MARKET or LIMIT
    @NotNull
    @Positive
    BigDecimal quantity;
    BigDecimal price;    // required for LIMIT orders
    BigDecimal stopPrice; // for STOP orders

    public enum OrderSide {
        BUY, SELL
    }

    public enum OrderType {
        MARKET, LIMIT, STOP_LIMIT, STOP_MARKET
    }
}
