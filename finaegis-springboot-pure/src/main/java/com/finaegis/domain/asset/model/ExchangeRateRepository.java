package com.finaegis.domain.asset.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("SELECT r FROM ExchangeRate r " +
           "WHERE r.fromAssetCode = :from AND r.toAssetCode = :to " +
           "AND r.active = true AND r.validAt <= :now " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now) " +
           "ORDER BY r.validAt DESC")
    Optional<ExchangeRate> findLatestValid(
        @Param("from") String from, @Param("to") String to, @Param("now") Instant now);
}
