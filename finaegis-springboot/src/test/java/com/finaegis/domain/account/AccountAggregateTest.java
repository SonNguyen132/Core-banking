package com.finaegis.domain.account;

import com.finaegis.domain.account.aggregate.AccountAggregate;
import com.finaegis.domain.account.command.CreateAccountCommand;
import com.finaegis.domain.account.command.DepositMoneyCommand;
import com.finaegis.domain.account.command.WithdrawMoneyCommand;
import com.finaegis.domain.account.event.AccountCreatedEvent;
import com.finaegis.domain.account.event.MoneyDepositedEvent;
import com.finaegis.domain.account.event.MoneyWithdrawnEvent;
import com.finaegis.domain.account.exception.InsufficientFundsException;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class AccountAggregateTest {

    private FixtureConfiguration<AccountAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(AccountAggregate.class);
    }

    @Test
    void shouldCreateAccount() {
        fixture.givenNoPriorActivity()
            .when(new CreateAccountCommand(
                "acc-1", "Savings", "user-1", "USD"))
            .expectEvents(new AccountCreatedEvent(
                "acc-1", "Savings", "user-1", "USD", null));
    }

    @Test
    void shouldRejectBlankName() {
        fixture.givenNoPriorActivity()
            .when(new CreateAccountCommand("acc-1", "  ", "user-1", "USD"))
            .expectException(IllegalArgumentException.class);
    }

    @Test
    void shouldDepositMoney() {
        fixture.given(new AccountCreatedEvent("acc-1", "Savings", "user-1", "USD", null))
            .when(new DepositMoneyCommand("acc-1", new BigDecimal("100.00"), "USD", "salary"))
            .expectEvents(new MoneyDepositedEvent(
                "acc-1", new BigDecimal("100.00"), "USD", "salary", null));
    }

    @Test
    void shouldWithdrawMoney() {
        fixture.given(
                new AccountCreatedEvent("acc-1", "Savings", "user-1", "USD", null),
                new MoneyDepositedEvent("acc-1", new BigDecimal("500.00"), "USD", "salary", null))
            .when(new WithdrawMoneyCommand("acc-1", new BigDecimal("200.00"), "USD", "rent"))
            .expectEvents(new MoneyWithdrawnEvent(
                "acc-1", new BigDecimal("200.00"), "USD", "rent", null));
    }

    @Test
    void shouldRejectOverdraft() {
        fixture.given(new AccountCreatedEvent("acc-1", "Savings", "user-1", "USD", null))
            .when(new WithdrawMoneyCommand("acc-1", new BigDecimal("100.00"), "USD", "rent"))
            .expectException(InsufficientFundsException.class);
    }
}
