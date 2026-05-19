package com.se1dhe.iqarena.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Настройки доставки outbox-событий в RabbitMQ.
@ConfigurationProperties(prefix = "app.outbox.rabbit")
public record OutboxRabbitProperties(
        boolean enabled,
        String exchange,
        String routingKeyPrefix,
        int maxAttempts,
        int retryBackoffSeconds,
        long confirmTimeoutMs
) {
}
