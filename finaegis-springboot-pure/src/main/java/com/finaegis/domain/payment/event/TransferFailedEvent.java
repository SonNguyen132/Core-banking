package com.finaegis.domain.payment.event;

import com.finaegis.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Emitted when a transfer fails permanently (including compensation already done).
 */
@NoArgsConstructor
@AllArgsConstructor
public class TransferFailedEvent extends DomainEvent {

    public String reason;
}
