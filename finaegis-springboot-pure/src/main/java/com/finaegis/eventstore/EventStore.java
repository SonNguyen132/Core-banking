package com.finaegis.eventstore;

import com.finaegis.domain.common.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persistence + publication of domain events.
 * - {@link #append(String, String, String, long, DomainEvent)} persists a single event.
 * - After append, event subscribers are notified so projections update.
 *
 * This lives on the write side (CQRS write model).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventStore {

    private final EventStoreRepository repository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    /**
     * Persist one event and publish it.
     *
     * @param streamId      logical stream for the aggregate
     * @param aggregateId   aggregate identifier
     * @param aggregateType aggregate type discriminator
     * @param expectedVersion expected current version for optimistic locking (0 for new aggregate)
     * @param event         the domain event
     */
    @Transactional
    public void append(String streamId, String aggregateId, String aggregateType,
                       long expectedVersion, DomainEvent event) {
        long current = repository.maxVersion(streamId);
        if (current != expectedVersion) {
            throw new ConcurrentModificationException(
                "Optimistic lock violated for stream " + streamId
                    + ": expected " + expectedVersion + " but found " + current);
        }

        long newVersion = current + 1;
        event.setAggregateId(aggregateId);
        event.setAggregateType(aggregateType);
        event.setVersion(newVersion);

        String json = writeEventData(event);

        StoredEvent stored = StoredEvent.create(
            streamId, aggregateId, aggregateType, event.getEventType(), newVersion, json);
        StoredEvent saved = repository.save(stored);
        event.setEventId(saved.getEventId());
        event.markRecorded();

        log.debug("Appended event {} v{} to stream {}", event.getEventType(), newVersion, streamId);
    }

    /**
     * Load all events of a stream (used to rebuild an aggregate).
     */
    public List<StoredEvent> loadStream(String streamId) {
        return repository.findByStreamIdOrderByEventVersionAsc(streamId);
    }

    /**
     * Publish captured but not-yet-persisted events after a whole aggregate command succeeds.
     */
    public void publishAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            if (event.isRecorded()) {
                eventPublisher.publish(event);
            }
        }
    }

    private String writeEventData(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot serialize event " + event.getEventType(), e);
        }
    }
}
