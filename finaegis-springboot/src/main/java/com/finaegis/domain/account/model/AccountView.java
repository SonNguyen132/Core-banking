package com.finaegis.domain.account.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountView {

    @Id
    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "asset_code", nullable = false, length = 10)
    private String assetCode;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean frozen = false;

    @Column(nullable = false)
    private boolean closed = false;

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
