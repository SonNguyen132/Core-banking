package com.finaegis.domain.asset.service;

import com.finaegis.domain.asset.model.ExchangeRate;
import com.finaegis.domain.asset.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal convertAmount(String from, String to, BigDecimal amount) {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        ExchangeRate rate = getRate(from, to);
        return amount.multiply(rate.getRate()).setScale(8, RoundingMode.HALF_UP);
    }

    public ExchangeRate getRate(String from, String to) {
        return exchangeRateRepository.findLatestValid(from, to, Instant.now())
            .orElseThrow(() -> new RuntimeException("Exchange rate not found: " + from + "/" + to));
    }

    public BigDecimal getRateValue(String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            return BigDecimal.ONE;
        }
        return getRate(from, to).getRate();
    }

    public ExchangeRate findRate(String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("Cannot fetch rate for same currency");
        }
        return getRate(from, to);
    }
}
