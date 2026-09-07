package com.finaegis.domain.stablecoin.service;

import com.finaegis.domain.stablecoin.model.StablecoinSupply;
import com.finaegis.domain.stablecoin.model.StablecoinSupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class StablecoinService {

    private final StablecoinSupplyRepository supplyRepository;

    public MintResult mint(String symbol, BigDecimal fiatAmount, String targetAccount) {
        if (fiatAmount == null || fiatAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mint amount must be positive");
        }
        StablecoinSupply supply = getOrCreate(symbol);
        supply.setReserveAmount(supply.getReserveAmount().add(fiatAmount));
        supply.setTotalSupply(supply.getTotalSupply().add(fiatAmount));
        supplyRepository.save(supply);
        return new MintResult(symbol, fiatAmount, targetAccount, supply.getTotalSupply());
    }

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

    public StablecoinSupply supply(String symbol) {
        return getOrCreate(symbol);
    }

    private StablecoinSupply getOrCreate(String symbol) {
        return supplyRepository.findById(symbol)
            .orElseGet(() -> StablecoinSupply.builder()
                .symbol(symbol)
                .totalSupply(BigDecimal.ZERO)
                .reserveAmount(BigDecimal.ZERO)
                .reserveAsset("USD")
                .minCollateralRatio(new BigDecimal("100.00"))
                .build());
    }

    public record MintResult(String symbol, BigDecimal amountMinted,
                             String creditedTo, BigDecimal newTotalSupply) {}
    public record BurnResult(String symbol, BigDecimal amountBurned,
                             String reserveReleasedTo, BigDecimal newTotalSupply) {}
}
