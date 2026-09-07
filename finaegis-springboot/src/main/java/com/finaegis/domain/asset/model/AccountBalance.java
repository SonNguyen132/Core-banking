package com.finaegis.domain.asset.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_balances",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_uuid", "asset_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_uuid", nullable = false, length = 36)
    private String accountUuid;

    @Column(name = "asset_code", nullable = false, length = 10)
    private String assetCode;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private Long version; // optimistic locking

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                "Insufficient balance in " + assetCode + ": " + balance + " < " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }
}
