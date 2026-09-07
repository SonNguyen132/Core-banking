package com.finaegis.domain.exchange.model;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderBookEntry {

    public enum Side { BUY, SELL }

    private final String orderId;
    private final String accountId;
    private final Side side;
    private BigDecimal remainingQuantity;
    private final BigDecimal price;
    private final long sequence;
    private final Instant timestamp;

    public OrderBookEntry(String orderId, String accountId, Side side,
                          BigDecimal quantity, BigDecimal price, long sequence) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.side = side;
        this.remainingQuantity = quantity;
        this.price = price;
        this.sequence = sequence;
        this.timestamp = Instant.now();
    }

    public void reduceQuantity(BigDecimal amount) {
        this.remainingQuantity = this.remainingQuantity.subtract(amount);
    }

    public boolean isFullyFilled() {
        return remainingQuantity.compareTo(BigDecimal.ZERO) <= 0;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public Side getSide() {
        return side;
    }

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public long getSequence() {
        return sequence;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
