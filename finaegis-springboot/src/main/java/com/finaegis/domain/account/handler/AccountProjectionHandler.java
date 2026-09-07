package com.finaegis.domain.account.handler;

import com.finaegis.domain.account.event.AccountClosedEvent;
import com.finaegis.domain.account.event.AccountCreatedEvent;
import com.finaegis.domain.account.event.AccountFrozenEvent;
import com.finaegis.domain.account.event.AccountUnfrozenEvent;
import com.finaegis.domain.account.event.MoneyDepositedEvent;
import com.finaegis.domain.account.event.MoneyWithdrawnEvent;
import com.finaegis.domain.account.model.AccountView;
import com.finaegis.domain.account.repository.AccountViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountProjectionHandler {

    private final AccountViewRepository repository;

    @EventHandler
    public void on(AccountCreatedEvent event) {
        AccountView view = AccountView.builder()
            .accountId(event.getAccountId())
            .name(event.getName())
            .userId(event.getUserId())
            .assetCode(event.getAssetCode())
            .build();
        repository.save(view);
        log.info("Account view created: {}", event.getAccountId());
    }

    @EventHandler
    public void on(MoneyDepositedEvent event) {
        repository.findById(event.getAccountId()).ifPresent(view -> {
            view.setBalance(view.getBalance().add(event.getAmount()));
            repository.save(view);
            log.info("Deposit applied to account {}: {}", event.getAccountId(), event.getAmount());
        });
    }

    @EventHandler
    public void on(MoneyWithdrawnEvent event) {
        repository.findById(event.getAccountId()).ifPresent(view -> {
            view.setBalance(view.getBalance().subtract(event.getAmount()));
            repository.save(view);
            log.info("Withdrawal applied to account {}: {}", event.getAccountId(), event.getAmount());
        });
    }

    @EventHandler
    public void on(AccountFrozenEvent event) {
        repository.findById(event.getAccountId()).ifPresent(view -> {
            view.setFrozen(true);
            repository.save(view);
            log.info("Account frozen: {}", event.getAccountId());
        });
    }

    @EventHandler
    public void on(AccountUnfrozenEvent event) {
        repository.findById(event.getAccountId()).ifPresent(view -> {
            view.setFrozen(false);
            repository.save(view);
            log.info("Account unfrozen: {}", event.getAccountId());
        });
    }

    @EventHandler
    public void on(AccountClosedEvent event) {
        repository.findById(event.getAccountId()).ifPresent(view -> {
            view.setClosed(true);
            repository.save(view);
            log.info("Account closed: {}", event.getAccountId());
        });
    }
}
