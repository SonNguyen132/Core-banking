package com.finaegis.domain.asset.service;

import com.finaegis.domain.asset.model.ExchangeRate;
import com.finaegis.domain.asset.model.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Exchange rate conversion with caching.
 */
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal convert(String from, String to, BigDecimal amount) {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        BigDecimal rate = findRate(from, to).rate;
        return amount.multiply(rate).setScale(8, RoundingMode.HALF_UP);
    }

    public Rate findRate(String from, String to) {
        return exchangeRateRepository.findLatestValid(from, to, Instant.now())
            .map(r -> new Rate(r.getFromAssetCode(), r.getToAssetCode(), r.getRate()))
            .orElseThrow(() -> new RuntimeException("Exchange rate not found: " + from + "/" + to));
    }

    public record Rate(String from, String to, BigDecimal rate) {}
}
