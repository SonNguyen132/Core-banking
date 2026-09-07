package com.finaegis.eventstore;

import com.finaegis.domain.common.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

/**
 * Generic repository to load and save any {@link AggregateRoot}.
 * Loads = replay events from store, Save = append uncommitted events with optimistic concurrency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AggregateRepository {

    private final EventStore eventStore;
    private final ObjectMapper objectMapper;

    /**
     * Load (or create) an aggregate by stream id.
     *
     * @param streamId         aggregate id
     * @param aggregateType    type discriminator used for event class resolution
     * @param factory          supplies a new empty aggregate if none exists yet
     */
    @Transactional(readOnly = true)
    public <T extends AggregateRoot> T load(String streamId, String aggregateType, Supplier<T> factory) {
        List<StoredEvent> stored = eventStore.loadStream(streamId);
        if (stored.isEmpty()) {
            T aggregate = factory.get();
            aggregate.setAggregateId(streamId);
            return aggregate;
        }
        T aggregate = factory.get();
        aggregate.rebuildFrom(stored, objectMapper);
        return aggregate;
    }

    /**
     * Persist all uncommitted events of the aggregate and publish them.
     */
    @Transactional
    public <T extends AggregateRoot> void save(T aggregate, long expectedVersion) {
        aggregate.setVersion(expectedVersion);
        List<DomainEvent> changes = aggregate.getUncommittedChanges();
        if (changes.isEmpty()) {
            return;
        }
        for (DomainEvent event : changes) {
            long next = aggregate.getVersion() + 1;
            eventStore.append(
                aggregate.streamId(),
                aggregate.getAggregateId(),
                aggregate.getAggregateType(),
                aggregate.getVersion(),
                event
            );
            aggregate.setVersion(next);
        }
        eventStore.publishAll(changes);
        aggregate.clearChanges();
    }
}
