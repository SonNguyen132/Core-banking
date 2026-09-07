package com.finaegis.domain.asset.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    public static final String TYPE_FIAT = "fiat";
    public static final String TYPE_CRYPTO = "crypto";
    public static final String TYPE_COMMODITY = "commodity";
    public static final String TYPE_CUSTOM = "custom";

    @Id
    @Column(length = 10)
    private String code; // USD, EUR, BTC, XAU

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private int precision; // 2 for USD, 8 for BTC

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(columnDefinition = "json")
    private String metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
