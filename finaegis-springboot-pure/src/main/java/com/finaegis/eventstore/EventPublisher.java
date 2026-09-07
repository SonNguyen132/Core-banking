package com.finaegis.eventstore;

import com.finaegis.domain.common.DomainEvent;

/**
 * Publishes recorded domain events to registered handlers (projections, sagas, external).
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
