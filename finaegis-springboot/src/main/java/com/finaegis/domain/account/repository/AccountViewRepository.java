package com.finaegis.domain.account.repository;

import com.finaegis.domain.account.model.AccountView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountViewRepository extends JpaRepository<AccountView, String> {

    List<AccountView> findByUserId(String userId);

    List<AccountView> findByUserIdAndClosedFalse(String userId);

    @Query("SELECT a FROM AccountView a WHERE a.userId = :userId AND a.closed = false AND a.frozen = false")
    List<AccountView> findActiveByUserId(@Param("userId") String userId);

    long countByClosedFalse();
}
