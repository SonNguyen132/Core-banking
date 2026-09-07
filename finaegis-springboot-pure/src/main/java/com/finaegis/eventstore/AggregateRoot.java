package com.finaegis.eventstore;

import com.finaegis.domain.common.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all event-sourced aggregates.
 *
 * Responsibilities:
 * - Hold the aggregate id + version.
 * - Track "uncommitted" events produced by command methods.
 * - Apply events to rebuild state ({@link #apply(DomainEvent)}).
 * - Rebuild from the event store by replaying stored events.
 *
 * This replaces Axon's AggregateRoot machinery.
 */
@Slf4j
public abstract class AggregateRoot {

    public static final String TYPE = "aggregate";

    private final String aggregateType;
    private String aggregateId;
    private long version;
    private final List<DomainEvent> changes = new ArrayList<>();

    protected AggregateRoot(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    /** Stream id = aggregate id (single-archetype aggregates share a stream). */
    public String streamId() {
        return aggregateId;
    }

    /**
     * Records an event produced by a command and applies it to the aggregate state
     * immediately (to guard invariants within the same command).
     */
    protected void apply(DomainEvent event) {
        event.setAggregateId(aggregateId);
        event.setAggregateType(aggregateType);
        changes.add(event);
        when(event);
    }

    /**
     * Dispatch event to the correct apply[Event] method via reflection.
     */
    protected void when(DomainEvent event) {
        String methodName = "on";
        try {
            Method method = this.getClass().getDeclaredMethod(methodName, event.getClass());
            method.setAccessible(true);
            method.invoke(this, event);
        } catch (NoSuchMethodException nsme) {
            log.warn("No apply handler for {} on {}", event.getEventType(), this.getClass().getSimpleName());
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to apply event " + event.getEventType() + " on " + this.getClass().getSimpleName(), e);
        }
    }

    /**
     * Reconstruct aggregate state from persisted events.
     */
    public void rebuildFrom(List<StoredEvent> storedEvents, ObjectMapper mapper) {
        for (StoredEvent stored : storedEvents) {
            this.aggregateId = stored.getAggregateId();
            this.version = stored.getEventVersion();
            DomainEvent event = deserialize(stored, mapper);
            when(event);
        }
    }

    private DomainEvent deserialize(StoredEvent stored, ObjectMapper mapper) {
        try {
            Class<?> clazz = Class.forName(stored.getAggregateType() + ".event." + stored.getEventType());
            Object obj = mapper.readValue(stored.getEventData(), clazz);
            DomainEvent event = (DomainEvent) obj;
            event.setAggregateId(stored.getAggregateId());
            event.setAggregateType(stored.getAggregateType());
            event.setVersion(stored.getEventVersion());
            event.setOccurredAt(stored.getCreatedAt());
            event.markRecorded();
            return event;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Cannot deserialize event " + stored.getEventType() + " from stream "
                    + stored.getStreamId(), e);
        }
    }

    public List<DomainEvent> getUncommittedChanges() {
        return new ArrayList<>(changes);
    }

    public void markChangesCommitted(List<DomainEvent> committed) {
        this.changes.removeAll(committed);
    }

    public void clearChanges() {
        this.changes.clear();
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
