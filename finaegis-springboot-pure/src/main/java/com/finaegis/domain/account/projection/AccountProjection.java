package com.finaegis.domain.account.projection;

import com.finaegis.domain.account.event.AccountClosedEvent;
import com.finaegis.domain.account.event.AccountCreatedEvent;
import com.finaegis.domain.account.event.AccountFrozenEvent;
import com.finaegis.domain.account.event.AccountUnfrozenEvent;
import com.finaegis.domain.account.event.MoneyDepositedEvent;
import com.finaegis.domain.account.event.MoneyWithdrawnEvent;
import com.finaegis.domain.account.model.AccountView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Projection - consumes domain events and updates the read model (account_view).
 * This separates the write side (aggregate/event store) from the read side (query).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountProjection {

    private final AccountViewRepository repository;

    @EventListener
    public void on(AccountCreatedEvent event) {
        AccountView view = AccountView.builder()
            .accountId(event.getAggregateId())
            .name(event.name)
            .userId(event.userId)
            .assetCode(event.assetCode)
            .build();
        repository.save(view);
        log.info("Account view created: {}", event.getAggregateId());
    }

    @EventListener
    public void on(MoneyDepositedEvent event) {
        repository.findById(event.getAggregateId()).ifPresent(view -> {
            view.setBalance(view.getBalance().add(event.amount));
            repository.save(view);
        });
    }

    @EventListener
    public void on(MoneyWithdrawnEvent event) {
        repository.findById(event.getAggregateId()).ifPresent(view -> {
            view.setBalance(view.getBalance().subtract(event.amount));
            repository.save(view);
        });
    }

    @EventListener
    public void on(AccountFrozenEvent event) {
        repository.findById(event.getAggregateId()).ifPresent(view -> {
            view.setFrozen(true);
            repository.save(view);
        });
    }

    @EventListener
    public void on(AccountUnfrozenEvent event) {
        repository.findById(event.getAggregateId()).ifPresent(view -> {
            view.setFrozen(false);
            repository.save(view);
        });
    }

    @EventListener
    public void on(AccountClosedEvent event) {
        repository.findById(event.getAggregateId()).ifPresent(view -> {
            view.setClosed(true);
            repository.save(view);
        });
    }
}
