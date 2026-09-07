package com.finaegis.domain.asset.model;

import jakarta.persistence.*;
import lombok.*;
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
    private java.math.BigDecimal rate;

    @Column(precision = 20, scale = 10)
    private java.math.BigDecimal bid;

    @Column(precision = 20, scale = 10)
    private java.math.BigDecimal ask;

    @Column(nullable = false, length = 50)
    private String source = "manual"; // manual, API, oracle, market

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "valid_at", nullable = false)
    private Instant validAt;

    @Column(columnDefinition = "json")
    private String metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (validAt == null) {
            validAt = Instant.now();
        }
        createdAt = Instant.now();
    }

    public boolean isValid() {
        if (!active) {
            return false;
        }
        if (validAt != null && validAt.isAfter(Instant.now())) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}
