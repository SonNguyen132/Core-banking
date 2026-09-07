package com.finaegis.domain.exchange.aggregate;

import com.finaegis.domain.exchange.command.CancelOrderCommand;
import com.finaegis.domain.exchange.command.PlaceOrderCommand;
import com.finaegis.domain.exchange.event.OrderCancelledEvent;
import com.finaegis.domain.exchange.event.OrderPlacedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
public class OrderAggregate {

    public enum OrderStatus {
        NEW, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED
    }

    @AggregateIdentifier
    private String orderId;
    private String accountId;
    private String symbol;
    private String side;
    private String type;
    private BigDecimal quantity;
    private BigDecimal filledQuantity = BigDecimal.ZERO;
    private BigDecimal price;
    private OrderStatus status;

    protected OrderAggregate() {
    }

    @CommandHandler
    public OrderAggregate(PlaceOrderCommand command) {
        if (command.getPrice() == null &&
            (command.getType() == PlaceOrderCommand.OrderType.LIMIT ||
             command.getType() == PlaceOrderCommand.OrderType.STOP_LIMIT)) {
            throw new IllegalArgumentException("Limit orders require a price");
        }

        AggregateLifecycle.apply(new OrderPlacedEvent(
            command.getOrderId(),
            command.getAccountId(),
            command.getSymbol(),
            command.getSide().name(),
            command.getType().name(),
            command.getQuantity(),
            command.getPrice(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(CancelOrderCommand command) {
        if (status == OrderStatus.FILLED || status == OrderStatus.CANCELLED) {
            return;
        }
        AggregateLifecycle.apply(new OrderCancelledEvent(
            command.getOrderId(),
            command.getReason(),
            Instant.now()
        ));
    }

    @EventSourcingHandler
    public void on(OrderPlacedEvent event) {
        this.orderId = event.getOrderId();
        this.accountId = event.getAccountId();
        this.symbol = event.getSymbol();
        this.side = event.getSide();
        this.type = event.getType();
        this.quantity = event.getQuantity();
        this.price = event.getPrice();
        this.filledQuantity = BigDecimal.ZERO;
        this.status = OrderStatus.NEW;
    }

    @EventSourcingHandler
    public void on(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
    }
}
