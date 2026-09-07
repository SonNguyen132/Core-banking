package com.finaegis.domain.account.aggregate;

import com.finaegis.domain.account.command.CloseAccountCommand;
import com.finaegis.domain.account.command.CreateAccountCommand;
import com.finaegis.domain.account.command.DepositMoneyCommand;
import com.finaegis.domain.account.command.FreezeAccountCommand;
import com.finaegis.domain.account.command.UnfreezeAccountCommand;
import com.finaegis.domain.account.command.WithdrawMoneyCommand;
import com.finaegis.domain.account.event.AccountClosedEvent;
import com.finaegis.domain.account.event.AccountCreatedEvent;
import com.finaegis.domain.account.event.AccountFrozenEvent;
import com.finaegis.domain.account.event.AccountUnfrozenEvent;
import com.finaegis.domain.account.event.MoneyDepositedEvent;
import com.finaegis.domain.account.event.MoneyWithdrawnEvent;
import com.finaegis.domain.account.exception.AccountClosedException;
import com.finaegis.domain.account.exception.AccountFrozenException;
import com.finaegis.domain.account.exception.InsufficientFundsException;
import com.finaegis.domain.account.exception.InvalidAmountException;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.time.Instant;

@Aggregate
public class AccountAggregate {

    @AggregateIdentifier
    private String accountId;
    private String name;
    private String userId;
    private String assetCode;
    private BigDecimal balance;
    private boolean frozen;
    private boolean closed;

    protected AccountAggregate() {
        // Required by Axon
    }

    @CommandHandler
    public AccountAggregate(CreateAccountCommand command) {
        if (command.getName() == null || command.getName().isBlank()) {
            throw new IllegalArgumentException("Account name is required");
        }
        if (command.getAssetCode() == null || command.getAssetCode().isBlank()) {
            throw new IllegalArgumentException("Asset code is required");
        }

        AggregateLifecycle.apply(new AccountCreatedEvent(
            command.getAccountId(),
            command.getName(),
            command.getUserId(),
            command.getAssetCode(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(DepositMoneyCommand command) {
        ensureActive();
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(command.getAmount());
        }

        AggregateLifecycle.apply(new MoneyDepositedEvent(
            command.getAccountId(),
            command.getAmount(),
            command.getCurrency(),
            command.getReference(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(WithdrawMoneyCommand command) {
        ensureActive();
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(command.getAmount());
        }
        if (balance.compareTo(command.getAmount()) < 0) {
            throw new InsufficientFundsException(balance, command.getAmount());
        }

        AggregateLifecycle.apply(new MoneyWithdrawnEvent(
            command.getAccountId(),
            command.getAmount(),
            command.getCurrency(),
            command.getReference(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(FreezeAccountCommand command) {
        ensureActive();
        if (frozen) {
            return; // Already frozen
        }
        AggregateLifecycle.apply(new AccountFrozenEvent(
            command.getAccountId(),
            command.getReason(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(UnfreezeAccountCommand command) {
        ensureOpen();
        if (!frozen) {
            return; // Already unfrozen
        }
        AggregateLifecycle.apply(new AccountUnfrozenEvent(
            command.getAccountId(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(CloseAccountCommand command) {
        if (closed) {
            return; // Already closed
        }
        if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot close account with positive balance");
        }
        AggregateLifecycle.apply(new AccountClosedEvent(
            command.getAccountId(),
            command.getReason(),
            Instant.now()
        ));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
        this.userId = event.getUserId();
        this.assetCode = event.getAssetCode();
        this.balance = BigDecimal.ZERO;
        this.frozen = false;
        this.closed = false;
    }

    @EventSourcingHandler
    public void on(MoneyDepositedEvent event) {
        this.balance = this.balance.add(event.getAmount());
    }

    @EventSourcingHandler
    public void on(MoneyWithdrawnEvent event) {
        this.balance = this.balance.subtract(event.getAmount());
    }

    @EventSourcingHandler
    public void on(AccountFrozenEvent event) {
        this.frozen = true;
    }

    @EventSourcingHandler
    public void on(AccountUnfrozenEvent event) {
        this.frozen = false;
    }

    @EventSourcingHandler
    public void on(AccountClosedEvent event) {
        this.closed = true;
    }

    // Getters for read access
    public String getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureActive() {
        ensureOpen();
        if (frozen) {
            throw new AccountFrozenException(accountId);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new AccountClosedException(accountId);
        }
    }
}
