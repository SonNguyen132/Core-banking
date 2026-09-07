package com.finaegis.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventStoreRepository extends JpaRepository<StoredEvent, String> {

    List<StoredEvent> findByStreamIdOrderByEventVersionAsc(String streamId);

    List<StoredEvent> findByEventType(String eventType);

    Optional<StoredEvent> findTopByStreamIdOrderByEventVersionDesc(String streamId);

    @Query("SELECT COALESCE(MAX(e.eventVersion), 0) FROM StoredEvent e WHERE e.streamId = :streamId")
    long maxVersion(@Param("streamId") String streamId);

    boolean existsByStreamId(String streamId);
}
