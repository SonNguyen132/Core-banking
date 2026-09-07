package com.finaegis.presentation.rest;

import com.finaegis.domain.exchange.command.CancelOrderCommand;
import com.finaegis.domain.exchange.command.PlaceOrderCommand;
import com.finaegis.domain.exchange.service.MatchingEngine;
import com.finaegis.domain.exchange.model.OrderBookEntry;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final CommandGateway commandGateway;
    private final MatchingEngine matchingEngine;

    @PostMapping("/orders")
    public ResponseEntity<Void> placeOrder(@RequestBody PlaceOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        commandGateway.sendAndWait(new PlaceOrderCommand(
            orderId,
            request.accountId(),
            request.symbol(),
            PlaceOrderCommand.OrderSide.valueOf(request.side()),
            PlaceOrderCommand.OrderType.valueOf(request.type()),
            request.quantity(),
            request.price(),
            request.stopPrice()
        ));

        // Feed into matching engine (simplified; production uses event-driven flow)
        OrderBookEntry entry = new OrderBookEntry(
            orderId,
            request.accountId(),
            OrderBookEntry.Side.valueOf(request.side()),
            request.quantity(),
            request.type().equals("MARKET")
                ? new BigDecimal("0") // market orders match against best price
                : request.price(),
            System.nanoTime()
        );

        if (request.side().equals("BUY")) {
            matchingEngine.addBuyOrder(request.symbol(), entry);
        } else {
            matchingEngine.addSellOrder(request.symbol(), entry);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> cancel(@PathVariable String orderId) {
        commandGateway.sendAndWait(new CancelOrderCommand(orderId, "user cancelled"));
        return ResponseEntity.ok().build();
    }

    public record PlaceOrderRequest(String accountId, String symbol, String side,
                                    String type, BigDecimal quantity,
                                    BigDecimal price, BigDecimal stopPrice) {}
}
