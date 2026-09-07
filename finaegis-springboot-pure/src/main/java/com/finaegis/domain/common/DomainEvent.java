package com.finaegis.domain.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * Base marker/contract for all domain events.
 * Concrete events carry their own payload (immutable).
 * Provides the metadata every stored event needs.
 */
public abstract class DomainEvent {

    /** Unique id for this event occurrence. */
    @JsonIgnore
    private String eventId;

    /** The aggregate this event belongs to. */
    @JsonIgnore
    private String aggregateId;

    /** Aggregate type discriminator for replay. */
    @JsonIgnore
    private String aggregateType;

    /** Monotonic per-stream version for optimistic concurrency. */
    @JsonIgnore
    private long version;

    @JsonIgnore
    private Instant occurredAt;

    @JsonIgnore
    private boolean recorded;

    public DomainEvent() {
        this.occurredAt = Instant.now();
    }

    // setters used by AggregateRoot and event store
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public void setVersion(long version) { this.version = version; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public void markRecorded() { this.recorded = true; }

    public String getEventId() { return eventId; }
    public String getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public long getVersion() { return version; }
    public Instant getOccurredAt() { return occurredAt; }
    public boolean isRecorded() { return recorded; }

    /** Concrete events return their simple class name as the persisted event type. */
    @JsonIgnore
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
