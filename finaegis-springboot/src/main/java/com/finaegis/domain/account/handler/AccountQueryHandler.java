package com.finaegis.domain.account.handler;

import com.finaegis.domain.account.model.AccountView;
import com.finaegis.domain.account.query.GetAccountQuery;
import com.finaegis.domain.account.query.GetAccountsByUserQuery;
import com.finaegis.domain.account.repository.AccountViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountQueryHandler {

    private final AccountViewRepository repository;

    @QueryHandler
    public AccountView handle(GetAccountQuery query) {
        return repository.findById(query.getAccountId())
            .orElseThrow(() -> new RuntimeException("Account not found: " + query.getAccountId()));
    }

    @QueryHandler
    public List<AccountView> handle(GetAccountsByUserQuery query) {
        return repository.findByUserId(query.getUserId());
    }
}
