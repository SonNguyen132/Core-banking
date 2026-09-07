package com.finaegis.domain.account.command;

import com.finaegis.domain.account.aggregate.Account;
import com.finaegis.eventstore.AggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Command-side application service (CQRS write side).
 * Coordinates: load aggregate -> run command -> save+publish events.
 * This mirrors the role of Axon's CommandGateway + CommandHandler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountCommandService {

    private final AggregateRepository aggregateRepository;

    public String create(String name, String userId, String assetCode) {
        String accountId = UUID.randomUUID().toString();
        Account account = aggregateRepository.load(accountId, Account.TYPE, Account::new);
        account.create(name, userId, assetCode);
        aggregateRepository.save(account, account.getVersion());
        return accountId;
    }

    public void deposit(String accountId, java.math.BigDecimal amount, String currency, String reference) {
        Account account = aggregateRepository.load(accountId, Account.TYPE, Account::new);
        account.deposit(amount, currency, reference);
        aggregateRepository.save(account, account.getVersion());
    }

    public void withdraw(String accountId, java.math.BigDecimal amount, String currency, String reference) {
        Account account = aggregateRepository.load(accountId, Account.TYPE, Account::new);
        account.withdraw(amount, currency, reference);
        aggregateRepository.save(account, account.getVersion());
    }

    public void freeze(String accountId, String reason) {
        Account account = aggregateRepository.load(accountId, Account.TYPE, Account::new);
        account.freeze(reason);
        aggregateRepository.save(account, account.getVersion());
    }

    public void unfreeze(String accountId) {
        Account account = aggregateRepository.load(accountId, Account.TYPE, Account::new);
        account.unfreeze();
        aggregateRepository.save(account, account.getVersion());
    }

    public void close(String accountId, String reason) {
        Account account = aggregateRepository.load(accountId, Account.TYPE, Account::new);
        account.close(reason);
        aggregateRepository.save(account, account.getVersion());
    }
}
