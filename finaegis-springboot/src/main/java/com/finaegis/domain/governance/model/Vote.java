package com.finaegis.domain.governance.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "votes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    private String id;

    @Column(name = "poll_id", nullable = false)
    private String pollId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "option_key", nullable = false)
    private String optionKey;

    @Column(nullable = false)
    private BigDecimal votingPower = BigDecimal.ONE;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
