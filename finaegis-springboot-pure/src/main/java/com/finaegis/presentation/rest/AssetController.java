package com.finaegis.presentation.rest;

import com.finaegis.domain.asset.model.Asset;
import com.finaegis.domain.asset.model.ExchangeRate;
import com.finaegis.domain.asset.model.ExchangeRateRepository;
import com.finaegis.domain.asset.service.AssetService;
import com.finaegis.domain.asset.service.ExchangeRateService;
import com.finaegis.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.ASSET_VIEW + "')")
    public ResponseEntity<List<Asset>> list() {
        return ResponseEntity.ok(assetService.listActive());
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ASSET_VIEW + "')")
    public ResponseEntity<Asset> get(@PathVariable String code) {
        return ResponseEntity.ok(assetService.find(code));
    }

    @GetMapping("/rates")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ASSET_RATE_VIEW + "')")
    public ResponseEntity<List<ExchangeRate>> rates() {
        return ResponseEntity.ok(exchangeRateRepository.findAll());
    }

    @GetMapping("/rates/{from}/{to}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ASSET_RATE_VIEW + "')")
    public ResponseEntity<ExchangeRateService.Rate> getRate(@PathVariable String from,
                                                            @PathVariable String to) {
        return ResponseEntity.ok(exchangeRateService.findRate(from, to));
    }

    @PostMapping("/rates/convert")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ASSET_CONVERT + "')")
    public ResponseEntity<ConversionResult> convert(@RequestBody ConversionRequest request) {
        BigDecimal converted = exchangeRateService.convert(request.from(), request.to(), request.amount());
        return ResponseEntity.ok(new ConversionResult(
            request.from(), request.to(), request.amount(), converted));
    }

    public record ConversionRequest(String from, String to, BigDecimal amount) {}
    public record ConversionResult(String from, String to,
                                   BigDecimal amount, BigDecimal converted) {}
}
