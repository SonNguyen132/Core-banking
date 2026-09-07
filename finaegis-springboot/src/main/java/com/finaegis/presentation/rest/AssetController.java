package com.finaegis.presentation.rest;

import com.finaegis.domain.asset.model.Asset;
import com.finaegis.domain.asset.model.ExchangeRate;
import com.finaegis.domain.asset.repository.AssetRepository;
import com.finaegis.domain.asset.repository.ExchangeRateRepository;
import com.finaegis.domain.asset.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetRepository assetRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ResponseEntity<List<Asset>> list() {
        return ResponseEntity.ok(assetRepository.findByActiveTrue());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Asset> get(@PathVariable String code) {
        return ResponseEntity.ok(assetRepository.findById(code)
            .orElseThrow(() -> new RuntimeException("Asset not found: " + code)));
    }

    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRate>> rates() {
        return ResponseEntity.ok(exchangeRateRepository.findAll());
    }

    @GetMapping("/rates/{from}/{to}")
    public ResponseEntity<ExchangeRate> getRate(@PathVariable String from, @PathVariable String to) {
        return ResponseEntity.ok(exchangeRateService
            .findRate(from, to));
    }

    @PostMapping("/rates/convert")
    public ResponseEntity<ConversionResult> convert(@RequestBody ConversionRequest request) {
        BigDecimal converted = exchangeRateService.convertAmount(
            request.from(), request.to(), request.amount());
        BigDecimal rate = exchangeRateService.getRateValue(request.from(), request.to());
        return ResponseEntity.ok(new ConversionResult(
            request.from(), request.to(), request.amount(), converted, rate));
    }

    // Expose rate lookup on service without repository dependency chain issue
    public record ConversionRequest(String from, String to, BigDecimal amount) {}
    public record ConversionResult(String from, String to,
                                   BigDecimal amount, BigDecimal converted, BigDecimal rate) {}
}
