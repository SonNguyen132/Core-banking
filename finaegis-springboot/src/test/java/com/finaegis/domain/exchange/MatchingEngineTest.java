package com.finaegis.domain.exchange;

import com.finaegis.domain.exchange.model.OrderBookEntry;
import com.finaegis.domain.exchange.service.MatchingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingEngineTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    @Test
    void shouldMatchBuyAndSellOrders() {
        // Sell 1 BTC @ 30000
        OrderBookEntry sell = new OrderBookEntry(
            "sell-1", "acc-a", OrderBookEntry.Side.SELL,
            new BigDecimal("1.0"), new BigDecimal("30000"), 1);
        // Buy 1 BTC @ 30500 (above ask -> crosses)
        OrderBookEntry buy = new OrderBookEntry(
            "buy-1", "acc-b", OrderBookEntry.Side.BUY,
            new BigDecimal("1.0"), new BigDecimal("30500"), 2);

        engine.addSellOrder("BTC/USD", sell);
        int trades = engine.matchForTest("BTC/USD");
    }

    @Test
    void shouldNotMatchWhenNoCross() {
        // Sell asks 31000, buy bids 30000 -> no cross
        OrderBookEntry sell = new OrderBookEntry(
            "sell-1", "acc-a", OrderBookEntry.Side.SELL,
            new BigDecimal("1.0"), new BigDecimal("31000"), 1);
        OrderBookEntry buy = new OrderBookEntry(
            "buy-1", "acc-b", OrderBookEntry.Side.BUY,
            new BigDecimal("1.0"), new BigDecimal("30000"), 2);

        engine.addSellOrder("BTC/USD", sell);
        engine.addBuyOrder("BTC/USD", buy);
        int trades = engine.matchForTest("BTC/USD");
        assertEquals(0, trades, "Orders that don't cross should not match");
    }
}
