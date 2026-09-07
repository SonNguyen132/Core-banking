package com.finaegis.domain.exchange.service;

import com.finaegis.domain.exchange.model.OrderBookEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pure application service for the exchange - places/cancels orders into the
 * in-memory matching engine. (Order matching is naturally stateful/in-memory;
 * a production system would persist fills via events in the event store.)
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final MatchingEngine matchingEngine;

    public String placeOrder(String accountId, String symbol, String side, String type,
                             BigDecimal quantity, BigDecimal price) {
        String orderId = UUID.randomUUID().toString();
        OrderBookEntry.Side entrySide = OrderBookEntry.Side.valueOf(side.toUpperCase());
        BigDecimal entryPrice = "MARKET".equalsIgnoreCase(type)
            ? BigDecimal.ZERO
            : price;

        OrderBookEntry entry = new OrderBookEntry(
            orderId, accountId, entrySide, quantity, entryPrice, System.nanoTime());

        if (entrySide == OrderBookEntry.Side.BUY) {
            matchingEngine.addBuyOrder(symbol, entry);
        } else {
            matchingEngine.addSellOrder(symbol, entry);
        }
        return orderId;
    }

    public int cancelOrder(String orderId) {
        // Orders are matched/filled immediately in this simplified engine;
        // cancellation of a resting order is a no-op returning 0 matches.
        return 0;
    }

    public MatchingEngine getMatchingEngine() {
        return matchingEngine;
    }
}
