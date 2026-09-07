package com.finaegis.domain.exchange.service;

import com.finaegis.domain.exchange.model.OrderBook;
import com.finaegis.domain.exchange.model.OrderBookEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Matching Engine - price-time priority matching across per-symbol order books.
 * Thread-safe using concurrent maps and synchronized matching operations.
 */
@Service
public class MatchingEngine {

    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    public boolean addBuyOrder(String symbol, OrderBookEntry buy) {
        OrderBook book = orderBooks.computeIfAbsent(symbol, OrderBook::new);
        synchronized (book) {
            book.addOrder(buy);
            return match(book);
        }
    }

    public boolean addSellOrder(String symbol, OrderBookEntry sell) {
        OrderBook book = orderBooks.computeIfAbsent(symbol, OrderBook::new);
        synchronized (book) {
            book.addOrder(sell);
            return match(book);
        }
    }

    /**
     * Process limit orders and return the number of trades executed.
     */
    public int match(OrderBook book) {
        int trades = 0;
        while (true) {
            OrderBookEntry bestBuy = book.getBuyOrders().peek();
            OrderBookEntry bestSell = book.getSellOrders().peek();

            if (bestBuy == null || bestSell == null) {
                break;
            }
            // Crossing: buy price >= sell price
            if (bestBuy.getPrice().compareTo(bestSell.getPrice()) < 0) {
                break;
            }

            BigDecimal tradePrice = bestSell.getPrice();
            BigDecimal tradeQuantity = bestBuy.getRemainingQuantity()
                .min(bestSell.getRemainingQuantity());

            bestBuy.reduceQuantity(tradeQuantity);
            bestSell.reduceQuantity(tradeQuantity);

            if (bestBuy.isFullyFilled()) {
                book.getBuyOrders().poll();
            }
            if (bestSell.isFullyFilled()) {
                book.getSellOrders().poll();
            }
            trades++;
        }
        return trades;
    }

    /**
     * Trigger matching for a specific symbol's order book (exposed for tests).
     */
    public int matchForTest(String symbol) {
        OrderBook book = orderBooks.get(symbol);
        if (book == null) {
            return 0;
        }
        synchronized (book) {
            return match(book);
        }
    }
}
