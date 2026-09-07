package com.finaegis.eventstore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A single persisted event in the event store.
 * This is the backbone of hand-written event sourcing.
 * Recorded events are immutable - this is the "source of truth".
 */
@Entity
@Table(name = "stored_events", indexes = {
    @Index(name = "idx_aggregate", columnList = "aggregate_id, aggregate_type"),
    @Index(name = "idx_stream", columnList = "stream_id"),
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoredEvent {

    @Id
    @Column(length = 36)
    private String eventId;

    @Column(name = "stream_id", nullable = false, length = 36)
    private String streamId;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private long eventVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", nullable = false, columnDefinition = "json")
    @Lob
    private String eventData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static StoredEvent create(String streamId, String aggregateId, String aggregateType,
                                     String eventType, long version, String eventDataJson) {
        return new StoredEvent(
            UUID.randomUUID().toString(),
            streamId,
            aggregateId,
            aggregateType,
            eventType,
            version,
            eventDataJson,
            Instant.now()
        );
    }
}
