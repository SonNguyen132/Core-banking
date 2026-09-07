package com.finaegis.domain.asset.service;

import com.finaegis.domain.asset.model.AccountBalance;
import com.finaegis.domain.asset.repository.AccountBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceService {

    private final AccountBalanceRepository accountBalanceRepository;

    public BigDecimal getBalance(String accountUuid, String assetCode) {
        return accountBalanceRepository
            .findByAccountUuidAndAssetCode(accountUuid, assetCode)
            .map(AccountBalance::getBalance)
            .orElse(BigDecimal.ZERO);
    }

    public List<AccountBalance> getBalances(String accountUuid) {
        return accountBalanceRepository.findByAccountUuid(accountUuid);
    }

    public void credit(String accountUuid, String assetCode, BigDecimal amount) {
        AccountBalance balance = accountBalanceRepository
            .findByAccountUuidAndAssetCode(accountUuid, assetCode)
            .orElseGet(() -> AccountBalance.builder()
                .accountUuid(accountUuid)
                .assetCode(assetCode)
                .balance(BigDecimal.ZERO)
                .build());
        balance.credit(amount);
        accountBalanceRepository.save(balance);
    }

    public void debit(String accountUuid, String assetCode, BigDecimal amount) {
        AccountBalance balance = accountBalanceRepository
            .findByAccountUuidAndAssetCode(accountUuid, assetCode)
            .orElseThrow(() -> new IllegalStateException(
                "No balance for " + assetCode + " on account " + accountUuid));
        balance.debit(amount);
        accountBalanceRepository.save(balance);
    }
}
