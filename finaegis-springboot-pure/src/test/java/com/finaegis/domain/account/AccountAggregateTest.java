package com.finaegis.domain.account;

import com.finaegis.domain.account.aggregate.Account;
import com.finaegis.domain.account.event.AccountCreatedEvent;
import com.finaegis.domain.account.event.MoneyDepositedEvent;
import com.finaegis.domain.common.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for pure event-sourced Account aggregate.
 * No Axon, no Spring context — just direct object manipulation.
 */
class AccountAggregateTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAggregateId("acc-1");
    }

    @Test
    void shouldCreateAccount() {
        account.create("Savings", "user-1", "USD");

        assertEquals("Savings", account.getName());
        assertEquals("user-1", account.getUserId());
        assertEquals("USD", account.getAssetCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        assertFalse(account.isFrozen());
        assertFalse(account.isClosed());

        List<DomainEvent> changes = account.getUncommittedChanges();
        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof AccountCreatedEvent);
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class,
            () -> account.create("  ", "user-1", "USD"));
    }

    @Test
    void shouldDepositMoney() {
        account.create("Savings", "user-1", "USD");
        account.deposit(new BigDecimal("100.00"), "USD", "salary");

        assertEquals(0, new BigDecimal("100.00").compareTo(account.getBalance()));
        assertEquals(2, account.getUncommittedChanges().size());
    }

    @Test
    void shouldWithdrawMoney() {
        account.create("Savings", "user-1", "USD");
        account.deposit(new BigDecimal("500.00"), "USD", "salary");
        account.withdraw(new BigDecimal("200.00"), "USD", "rent");

        assertEquals(0, new BigDecimal("300.00").compareTo(account.getBalance()));
        assertEquals(3, account.getUncommittedChanges().size());
    }

    @Test
    void shouldRejectOverdraft() {
        account.create("Savings", "user-1", "USD");
        assertThrows(IllegalStateException.class,
            () -> account.withdraw(new BigDecimal("100.00"), "USD", "rent"));
    }

    @Test
    void shouldRejectDepositWhenClosed() {
        account.create("Savings", "user-1", "USD");
        account.close("closing");
        assertThrows(IllegalStateException.class,
            () -> account.deposit(new BigDecimal("50.00"), "USD", "test"));
    }

    @Test
    void shouldRejectWithdrawWhenFrozen() {
        account.create("Savings", "user-1", "USD");
        account.deposit(new BigDecimal("500.00"), "USD", "salary");
        account.freeze("audit");
        assertThrows(IllegalStateException.class,
            () -> account.withdraw(new BigDecimal("100.00"), "USD", "rent"));
    }

    @Test
    void shouldAllowWithdrawAfterUnfreeze() {
        account.create("Savings", "user-1", "USD");
        account.deposit(new BigDecimal("500.00"), "USD", "salary");
        account.freeze("audit");
        account.unfreeze();
        account.withdraw(new BigDecimal("100.00"), "USD", "rent");
        assertEquals(0, new BigDecimal("400.00").compareTo(account.getBalance()));
    }

    @Test
    void shouldFreezeAndUnfreeze() {
        account.create("Savings", "user-1", "USD");
        assertFalse(account.isFrozen());
        account.freeze("audit");
        assertTrue(account.isFrozen());
        account.unfreeze();
        assertFalse(account.isFrozen());
    }

    @Test
    void shouldCloseEmptyAccount() {
        account.create("Savings", "user-1", "USD");
        account.close("user requested");
        assertTrue(account.isClosed());
        assertEquals(2, account.getUncommittedChanges().size());
    }

    @Test
    void shouldRejectClosingAccountWithBalance() {
        account.create("Savings", "user-1", "USD");
        account.deposit(new BigDecimal("100.00"), "USD", "deposit");
        assertThrows(IllegalStateException.class,
            () -> account.close("user requested"));
    }

    @Test
    void shouldSerializeAndDeserializeEvent() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        AccountCreatedEvent event = new AccountCreatedEvent("Savings", "user-1", "USD");
        event.setAggregateId("acc-1");
        event.setAggregateType(Account.TYPE);
        event.setVersion(1);

        String json = mapper.writeValueAsString(event);
        AccountCreatedEvent deserialized = mapper.readValue(json, AccountCreatedEvent.class);

        assertEquals("Savings", deserialized.name);
        assertEquals("user-1", deserialized.userId);
        assertEquals("USD", deserialized.assetCode);
    }
}
