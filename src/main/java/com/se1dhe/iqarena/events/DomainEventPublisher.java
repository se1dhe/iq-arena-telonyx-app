package com.se1dhe.iqarena.events;

import java.util.Map;
import java.util.UUID;

public interface DomainEventPublisher {
    void publish(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload);
}
