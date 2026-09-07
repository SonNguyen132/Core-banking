package com.finaegis.domain.asset.repository;

import com.finaegis.domain.asset.model.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    Optional<AccountBalance> findByAccountUuidAndAssetCode(String accountUuid, String assetCode);

    List<AccountBalance> findByAccountUuid(String accountUuid);
}
