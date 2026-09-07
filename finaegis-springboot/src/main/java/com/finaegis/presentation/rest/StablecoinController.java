package com.finaegis.presentation.rest;

import com.finaegis.domain.stablecoin.model.StablecoinSupply;
import com.finaegis.domain.stablecoin.service.StablecoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/stablecoins")
@RequiredArgsConstructor
public class StablecoinController {

    private final StablecoinService stablecoinService;

    @PostMapping("/mint")
    public ResponseEntity<StablecoinService.MintResult> mint(@RequestBody MintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            stablecoinService.mint(request.symbol(), request.amount(), request.targetAccount())
        );
    }

    @PostMapping("/burn")
    public ResponseEntity<StablecoinService.BurnResult> burn(@RequestBody MintRequest request) {
        return ResponseEntity.ok(
            stablecoinService.burn(request.symbol(), request.amount(), request.targetAccount())
        );
    }

    @GetMapping("/{symbol}/supply")
    public ResponseEntity<StablecoinSupply> supply(@PathVariable String symbol) {
        return ResponseEntity.ok(stablecoinService.getSupply(symbol));
    }

    @GetMapping("/{symbol}/collateral-ratio")
    public ResponseEntity<RatioResult> collateralRatio(@PathVariable String symbol) {
        StablecoinSupply supply = stablecoinService.getSupply(symbol);
        return ResponseEntity.ok(new RatioResult(
            symbol,
            supply.currentCollateralRatio(),
            supply.getMinCollateralRatio(),
            supply.isUnderCollateralized()
        ));
    }

    public record MintRequest(String symbol, BigDecimal amount, String targetAccount) {}
    public record RatioResult(String symbol, BigDecimal currentRatio,
                              BigDecimal minRatio, boolean underCollateralized) {}
}
