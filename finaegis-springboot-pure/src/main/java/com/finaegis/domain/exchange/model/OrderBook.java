package com.finaegis.domain.exchange.model;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory order book per symbol.
 * Buy orders: highest price first (max-heap), then earliest sequence.
 * Sell orders: lowest price first (min-heap), then earliest sequence.
 */
public class OrderBook {

    private final String symbol;
    private final PriorityQueue<OrderBookEntry> buyOrders;
    private final PriorityQueue<OrderBookEntry> sellOrders;
    private final AtomicLong sequence = new AtomicLong();

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrders = new PriorityQueue<>(Comparator
            .comparing(OrderBookEntry::getPrice).reversed()
            .thenComparing(OrderBookEntry::getSequence));
        this.sellOrders = new PriorityQueue<>(Comparator
            .comparing(OrderBookEntry::getPrice)
            .thenComparing(OrderBookEntry::getSequence));
    }

    public void addOrder(OrderBookEntry entry) {
        if (entry.getSide() == OrderBookEntry.Side.BUY) {
            buyOrders.offer(entry);
        } else {
            sellOrders.offer(entry);
        }
    }

    public long nextSequence() {
        return sequence.incrementAndGet();
    }

    public PriorityQueue<OrderBookEntry> getBuyOrders() {
        return buyOrders;
    }

    public PriorityQueue<OrderBookEntry> getSellOrders() {
        return sellOrders;
    }

    public String getSymbol() {
        return symbol;
    }
}
