package com.finaegis.eventstore;

import com.finaegis.domain.common.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationEventPublisher based event dispatcher.
 * Domain events are rebroadcast into the Spring event bus so that
 * @EventListener subscribers (projections, sagas) react to them.
 */
@Component
@Slf4j
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
        log.debug("Published domain event {}", event.getEventType());
    }
}
