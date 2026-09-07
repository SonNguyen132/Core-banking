package com.finaegis.domain.stablecoin.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stablecoin_supply")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StablecoinSupply {

    @Id
    @Column(length = 20)
    private String symbol; // e.g. USDC, GCU

    @Column(name = "total_supply", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalSupply = BigDecimal.ZERO;

    @Column(name = "reserve_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal reserveAmount = BigDecimal.ZERO;

    @Column(name = "reserve_asset", nullable = false, length = 10)
    private String reserveAsset = "USD";

    @Column(name = "min_collateral_ratio", nullable = false, precision = 8, scale = 2)
    private BigDecimal minCollateralRatio = new BigDecimal("200.00");

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public BigDecimal currentCollateralRatio() {
        if (totalSupply.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return reserveAmount.multiply(new BigDecimal("100"))
            .divide(totalSupply, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean isUnderCollateralized() {
        return currentCollateralRatio().compareTo(minCollateralRatio) < 0;
    }
}
