package com.se1dhe.iqarena.events;

// Статус события в transactional outbox.
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
