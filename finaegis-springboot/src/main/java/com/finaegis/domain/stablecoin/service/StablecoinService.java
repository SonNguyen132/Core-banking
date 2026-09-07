package com.finaegis.domain.stablecoin.service;

import com.finaegis.domain.stablecoin.model.StablecoinSupply;
import com.finaegis.domain.stablecoin.repository.StablecoinSupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StablecoinService {

    private final StablecoinSupplyRepository supplyRepository;

    /**
     * Mint new stablecoin tokens backed 1:1 by reserve asset.
     */
    public MintResult mint(String symbol, BigDecimal fiatAmount, String targetAccount) {
        if (fiatAmount == null || fiatAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mint amount must be positive");
        }

        StablecoinSupply supply = getOrCreate(symbol);

        // Increase reserve and supply in lock-step (1:1)
        supply.setReserveAmount(supply.getReserveAmount().add(fiatAmount));
        supply.setTotalSupply(supply.getTotalSupply().add(fiatAmount));
        supplyRepository.save(supply);

        return new MintResult(symbol, fiatAmount, targetAccount, supply.getTotalSupply());
    }

    /**
     * Burn stablecoin tokens and release reserve back to user (fractional).
     */
    public BurnResult burn(String symbol, BigDecimal tokenAmount, String targetAccount) {
        if (tokenAmount == null || tokenAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Burn amount must be positive");
        }

        StablecoinSupply supply = getOrCreate(symbol);

        if (supply.getTotalSupply().compareTo(tokenAmount) < 0) {
            throw new IllegalStateException("Insufficient token supply to burn");
        }

        supply.setTotalSupply(supply.getTotalSupply().subtract(tokenAmount));
        supply.setReserveAmount(supply.getReserveAmount().subtract(tokenAmount));
        supplyRepository.save(supply);

        return new BurnResult(symbol, tokenAmount, targetAccount, supply.getTotalSupply());
    }

    public StablecoinSupply getSupply(String symbol) {
        return getOrCreate(symbol);
    }

    private StablecoinSupply getOrCreate(String symbol) {
        return supplyRepository.findById(symbol)
            .orElseGet(() -> StablecoinSupply.builder()
                .symbol(symbol)
                .totalSupply(BigDecimal.ZERO)
                .reserveAmount(BigDecimal.ZERO)
                .reserveAsset("USD")
                .minCollateralRatio(new BigDecimal("200.00"))
                .build());
    }

    public record MintResult(String symbol, BigDecimal amountMinted,
                             String creditedTo, BigDecimal newTotalSupply) {
    }

    public record BurnResult(String symbol, BigDecimal amountBurned,
                             String reserveReleasedTo, BigDecimal newTotalSupply) {
    }
}
