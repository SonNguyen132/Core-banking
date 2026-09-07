package com.finaegis.presentation.rest;

import com.finaegis.domain.stablecoin.model.StablecoinSupply;
import com.finaegis.domain.stablecoin.service.StablecoinService;
import com.finaegis.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/stablecoins")
@RequiredArgsConstructor
public class StablecoinController {

    private final StablecoinService stablecoinService;

    @PostMapping("/mint")
    @PreAuthorize("hasAuthority('" + PermissionConstants.STABLECOIN_MINT + "')")
    public ResponseEntity<StablecoinService.MintResult> mint(@RequestBody MintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            stablecoinService.mint(request.symbol(), request.amount(), request.targetAccount()));
    }

    @PostMapping("/burn")
    @PreAuthorize("hasAuthority('" + PermissionConstants.STABLECOIN_BURN + "')")
    public ResponseEntity<StablecoinService.BurnResult> burn(@RequestBody MintRequest request) {
        return ResponseEntity.ok(
            stablecoinService.burn(request.symbol(), request.amount(), request.targetAccount()));
    }

    @GetMapping("/{symbol}/supply")
    @PreAuthorize("hasAuthority('" + PermissionConstants.STABLECOIN_VIEW + "')")
    public ResponseEntity<StablecoinSupply> supply(@PathVariable String symbol) {
        return ResponseEntity.ok(stablecoinService.supply(symbol));
    }

    @GetMapping("/{symbol}/collateral-ratio")
    @PreAuthorize("hasAuthority('" + PermissionConstants.STABLECOIN_VIEW + "')")
    public ResponseEntity<RatioResult> collateralRatio(@PathVariable String symbol) {
        StablecoinSupply supply = stablecoinService.supply(symbol);
        return ResponseEntity.ok(new RatioResult(
            symbol,
            supply.currentCollateralRatio(),
            supply.getMinCollateralRatio(),
            supply.isUnderCollateralized()));
    }

    public record MintRequest(String symbol, BigDecimal amount, String targetAccount) {}
    public record RatioResult(String symbol, BigDecimal currentRatio,
                              BigDecimal minRatio, boolean underCollateralized) {}
}
