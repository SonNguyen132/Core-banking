package com.finaegis.domain.stablecoin;

import com.finaegis.domain.stablecoin.model.StablecoinSupply;
import com.finaegis.domain.stablecoin.model.StablecoinSupplyRepository;
import com.finaegis.domain.stablecoin.service.StablecoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StablecoinServiceTest {

    @Mock
    private StablecoinSupplyRepository supplyRepository;

    private StablecoinService stablecoinService;

    @BeforeEach
    void setUp() {
        stablecoinService = new StablecoinService(supplyRepository);
    }

    @Test
    void shouldMintTokenOneToOne() {
        when(supplyRepository.findById("USDC")).thenReturn(Optional.of(
            StablecoinSupply.builder()
                .symbol("USDC")
                .totalSupply(BigDecimal.ZERO)
                .reserveAmount(BigDecimal.ZERO)
                .reserveAsset("USD")
                .build()));
        when(supplyRepository.save(any(StablecoinSupply.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        StablecoinService.MintResult result = stablecoinService.mint(
            "USDC", new BigDecimal("1000"), "acc-1");

        assertEquals(new BigDecimal("1000"), result.amountMinted());
        assertEquals(new BigDecimal("1000"), result.newTotalSupply());
    }

    @Test
    void shouldRejectNonPositiveMint() {
        assertThrows(IllegalArgumentException.class,
            () -> stablecoinService.mint("USDC", new BigDecimal("-5"), "acc-1"));
    }

    @Test
    void shouldCalculateCollateralRatio() {
        StablecoinSupply supply = StablecoinSupply.builder()
            .symbol("USDC")
            .totalSupply(new BigDecimal("200"))
            .reserveAmount(new BigDecimal("100"))
            .reserveAsset("USD")
            .minCollateralRatio(new BigDecimal("100"))
            .build();

        assertEquals(new BigDecimal("50.00"), supply.currentCollateralRatio());
        assertTrue(supply.isUnderCollateralized());
    }

    @Test
    void shouldCalculateCollateralRatioNotUnder() {
        StablecoinSupply supply = StablecoinSupply.builder()
            .symbol("USDC")
            .totalSupply(new BigDecimal("100"))
            .reserveAmount(new BigDecimal("150"))
            .reserveAsset("USD")
            .minCollateralRatio(new BigDecimal("100"))
            .build();

        assertEquals(new BigDecimal("150.00"), supply.currentCollateralRatio());
        assertFalse(supply.isUnderCollateralized());
    }

    @Test
    void shouldBurnToken() {
        StablecoinSupply supply = StablecoinSupply.builder()
            .symbol("USDC")
            .totalSupply(new BigDecimal("1000"))
            .reserveAmount(new BigDecimal("1000"))
            .reserveAsset("USD")
            .minCollateralRatio(new BigDecimal("100"))
            .build();
        when(supplyRepository.findById("USDC")).thenReturn(Optional.of(supply));
        when(supplyRepository.save(any(StablecoinSupply.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        StablecoinService.BurnResult result = stablecoinService.burn(
            "USDC", new BigDecimal("300"), "acc-1");

        assertEquals(new BigDecimal("300"), result.amountBurned());
        assertEquals(new BigDecimal("700"), result.newTotalSupply());
    }

    @Test
    void shouldRejectBurnExceedingSupply() {
        StablecoinSupply supply = StablecoinSupply.builder()
            .symbol("USDC")
            .totalSupply(new BigDecimal("100"))
            .reserveAmount(new BigDecimal("100"))
            .reserveAsset("USD")
            .minCollateralRatio(new BigDecimal("100"))
            .build();
        when(supplyRepository.findById("USDC")).thenReturn(Optional.of(supply));

        assertThrows(IllegalStateException.class,
            () -> stablecoinService.burn("USDC", new BigDecimal("200"), "acc-1"));
    }
}
