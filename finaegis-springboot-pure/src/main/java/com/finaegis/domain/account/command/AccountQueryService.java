package com.finaegis.domain.account.command;

import com.finaegis.domain.account.model.AccountView;
import com.finaegis.domain.account.projection.AccountViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Query-side service (CQRS read side) - reads from the projection.
 */
@Service
@RequiredArgsConstructor
public class AccountQueryService {

    private final AccountViewRepository repository;

    public AccountView get(String accountId) {
        return repository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }

    public List<AccountView> listByUser(String userId) {
        return repository.findByUserId(userId);
    }
}
