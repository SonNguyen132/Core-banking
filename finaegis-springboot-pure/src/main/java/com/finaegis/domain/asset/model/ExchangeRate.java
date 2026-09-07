package com.finaegis.domain.asset.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_asset_code", nullable = false, length = 10)
    private String fromAssetCode;

    @Column(name = "to_asset_code", nullable = false, length = 10)
    private String toAssetCode;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "valid_at", nullable = false)
    private Instant validAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
