package com.finaegis.domain.account.aggregate;

import com.finaegis.domain.account.event.AccountClosedEvent;
import com.finaegis.domain.account.event.AccountCreatedEvent;
import com.finaegis.domain.account.event.AccountFrozenEvent;
import com.finaegis.domain.account.event.AccountUnfrozenEvent;
import com.finaegis.domain.account.event.MoneyDepositedEvent;
import com.finaegis.domain.account.event.MoneyWithdrawnEvent;
import com.finaegis.eventstore.AggregateRoot;

import java.math.BigDecimal;

/**
 * Account aggregate - pure event sourcing.
 * State is rebuilt by replaying events via {@link #apply}.
 */
public class Account extends AggregateRoot {

    public static final String TYPE = "com.finaegis.domain.account";

    private String name;
    private String userId;
    private String assetCode;
    private BigDecimal balance = BigDecimal.ZERO;
    private boolean frozen;
    private boolean closed;

    public Account() {
        super(TYPE);
    }

    // ---- Command methods ----

    public void create(String name, String userId, String assetCode) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account name is required");
        }
        if (assetCode == null || assetCode.isBlank()) {
            throw new IllegalArgumentException("Asset code is required");
        }
        apply(new AccountCreatedEvent(name, userId, assetCode));
    }

    public void deposit(BigDecimal amount, String currency, String reference) {
        requireActive();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        apply(new MoneyDepositedEvent(amount, currency, reference));
    }

    public void withdraw(BigDecimal amount, String currency, String reference) {
        requireActive();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                "Insufficient funds: balance " + balance + " < requested " + amount);
        }
        apply(new MoneyWithdrawnEvent(amount, currency, reference));
    }

    public void freeze(String reason) {
        requireOpen();
        if (!frozen) {
            apply(new AccountFrozenEvent(reason));
        }
    }

    public void unfreeze() {
        if (frozen) {
            apply(new AccountUnfrozenEvent());
        }
    }

    public void close(String reason) {
        if (closed) {
            return;
        }
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot close account with positive balance");
        }
        apply(new AccountClosedEvent(reason));
    }

    // ---- Event handlers (replay state) ----

    void on(AccountCreatedEvent event) {
        this.name = event.name;
        this.userId = event.userId;
        this.assetCode = event.assetCode;
        this.balance = BigDecimal.ZERO;
        this.frozen = false;
        this.closed = false;
    }

    void on(MoneyDepositedEvent event) {
        this.balance = this.balance.add(event.amount);
    }

    void on(MoneyWithdrawnEvent event) {
        this.balance = this.balance.subtract(event.amount);
    }

    void on(AccountFrozenEvent event) {
        this.frozen = true;
    }

    void on(AccountUnfrozenEvent event) {
        this.frozen = false;
    }

    void on(AccountClosedEvent event) {
        this.closed = true;
    }

    // ---- Guards ----

    private void requireActive() {
        requireOpen();
        if (frozen) {
            throw new IllegalStateException("Account " + getAggregateId() + " is frozen");
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Account " + getAggregateId() + " is closed");
        }
    }

    // ---- Getters ----

    public String getName() { return name; }
    public String getUserId() { return userId; }
    public String getAssetCode() { return assetCode; }
    public BigDecimal getBalance() { return balance; }
    public boolean isFrozen() { return frozen; }
    public boolean isClosed() { return closed; }
}
