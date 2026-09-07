package com.finaegis.domain.account.projection;

import com.finaegis.domain.account.model.AccountView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountViewRepository extends JpaRepository<AccountView, String> {

    List<AccountView> findByUserId(String userId);

    List<AccountView> findByUserIdAndClosedFalse(String userId);
}
