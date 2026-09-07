package com.finaegis.domain.asset.repository;

import com.finaegis.domain.asset.model.ExchangeRate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Cacheable(value = "rates", key = "#fromAsset + ':' + #toAsset")
    @Query("SELECT r FROM ExchangeRate r " +
           "WHERE r.fromAssetCode = :fromAsset " +
           "AND r.toAssetCode = :toAsset " +
           "AND r.active = true " +
           "AND r.validAt <= :now " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now) " +
           "ORDER BY r.validAt DESC")
    Optional<ExchangeRate> findLatestValid(
        @Param("fromAsset") String fromAsset,
        @Param("toAsset") String toAsset,
        @Param("now") Instant now);

    @Query("SELECT r FROM ExchangeRate r " +
           "WHERE r.fromAssetCode = :fromAsset AND r.toAssetCode = :toAsset " +
           "AND r.active = true " +
           "AND r.validAt <= :now " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now) " +
           "ORDER BY r.validAt DESC")
    Optional<ExchangeRate> findLatestValidForConversion(
        @Param("fromAsset") String fromAsset,
        @Param("toAsset") String toAsset,
        @Param("now") Instant now);
}
