package com.se1dhe.iqarena.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Пишет событие в outbox; внешний publisher сможет безопасно доставлять его в RabbitMQ.
@Service
@RequiredArgsConstructor
public class OutboxDomainEventPublisher implements DomainEventPublisher {
    private final OutboxEventRepository repository;

    @Override
    @Transactional
    public void publish(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(new LinkedHashMap<>(payload));
        event.setOccurredAt(Instant.now());
        event.setNextAttemptAt(event.getOccurredAt());
        repository.save(event);
    }
}
