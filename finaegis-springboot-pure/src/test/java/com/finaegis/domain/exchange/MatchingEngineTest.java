package com.finaegis.domain.exchange;

import com.finaegis.domain.exchange.model.OrderBookEntry;
import com.finaegis.domain.exchange.service.MatchingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    @Test
    void shouldMatchBuyAndSellOrders() {
        OrderBookEntry buy = new OrderBookEntry(
            "buy-1", "acc-b", OrderBookEntry.Side.BUY,
            new BigDecimal("1.0"), new BigDecimal("30500"), 1);
        OrderBookEntry sell = new OrderBookEntry(
            "sell-1", "acc-a", OrderBookEntry.Side.SELL,
            new BigDecimal("1.0"), new BigDecimal("30000"), 2);

        engine.addBuyOrder("BTC/USD", buy);
        int trades = engine.addSellOrder("BTC/USD", sell);

        assertEquals(1, trades);
        assertTrue(sell.isFullyFilled());
        assertTrue(buy.isFullyFilled());
    }

    @Test
    void shouldNotMatchWhenNoCross() {
        OrderBookEntry sell = new OrderBookEntry(
            "sell-1", "acc-a", OrderBookEntry.Side.SELL,
            new BigDecimal("1.0"), new BigDecimal("31000"), 1);
        OrderBookEntry buy = new OrderBookEntry(
            "buy-1", "acc-b", OrderBookEntry.Side.BUY,
            new BigDecimal("1.0"), new BigDecimal("30000"), 2);

        engine.addSellOrder("BTC/USD", sell);
        int trades = engine.addBuyOrder("BTC/USD", buy);

        assertEquals(0, trades);
        assertFalse(sell.isFullyFilled());
        assertFalse(buy.isFullyFilled());
    }

    @Test
    void shouldPartiallyFillLargerOrder() {
        OrderBookEntry sell = new OrderBookEntry(
            "sell-1", "acc-a", OrderBookEntry.Side.SELL,
            new BigDecimal("0.5"), new BigDecimal("30000"), 1);
        OrderBookEntry buy = new OrderBookEntry(
            "buy-1", "acc-b", OrderBookEntry.Side.BUY,
            new BigDecimal("2.0"), new BigDecimal("30000"), 2);

        engine.addSellOrder("BTC/USD", sell);
        int trades = engine.addBuyOrder("BTC/USD", buy);

        assertEquals(1, trades);
        assertTrue(sell.isFullyFilled());
        assertEquals(0, new BigDecimal("1.5").compareTo(buy.getRemainingQuantity()));
    }

    @Test
    void shouldMatchAtBestPrice() {
        // Two sell orders: 29000 and 30000
        OrderBookEntry sell1 = new OrderBookEntry(
            "sell-1", "acc-a", OrderBookEntry.Side.SELL,
            new BigDecimal("1.0"), new BigDecimal("29000"), 1);
        OrderBookEntry sell2 = new OrderBookEntry(
            "sell-2", "acc-b", OrderBookEntry.Side.SELL,
            new BigDecimal("1.0"), new BigDecimal("30000"), 2);
        // One buy at 30000 crosses both
        OrderBookEntry buy = new OrderBookEntry(
            "buy-1", "acc-c", OrderBookEntry.Side.BUY,
            new BigDecimal("2.0"), new BigDecimal("30000"), 3);

        engine.addSellOrder("BTC/USD", sell1);
        engine.addSellOrder("BTC/USD", sell2);
        int trades = engine.addBuyOrder("BTC/USD", buy);

        assertEquals(2, trades);
        assertTrue(sell1.isFullyFilled());
        assertTrue(sell2.isFullyFilled());
        assertTrue(buy.isFullyFilled());
    }
}
