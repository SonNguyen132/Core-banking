package com.finaegis.domain.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfer_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferView {

    public enum TransferStatus {
        PENDING, COMPLETED, FAILED, ROLLED_BACK
    }

    @Id
    @Column(name = "transfer_id", length = 36)
    private String transferId;

    @Column(name = "from_account_id", nullable = false, length = 36)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false, length = 36)
    private String toAccountId;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "initiated_by")
    private String initiatedBy;

    @Column(name = "created_at")
    private Instant createdAt;
}
