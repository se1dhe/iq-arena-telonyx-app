package com.se1dhe.iqarena.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop100ByStatusOrderByOccurredAtAsc(OutboxEventStatus status);

    List<OutboxEventEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
            OutboxEventStatus status,
            Instant now
    );

    long countByStatus(OutboxEventStatus status);
}
