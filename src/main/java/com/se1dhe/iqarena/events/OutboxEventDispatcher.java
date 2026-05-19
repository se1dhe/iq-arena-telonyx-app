package com.se1dhe.iqarena.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

// Доставляет pending-события из transactional outbox в RabbitMQ.
@Slf4j
@Service
public class OutboxEventDispatcher {
    private final OutboxEventRepository repository;
    private final RabbitOperations rabbitOperations;
    private final OutboxRabbitProperties properties;
    private final Counter publishedCounter;
    private final Counter retryCounter;
    private final Counter failedCounter;
    private final Timer publishTimer;
    private final ObservationRegistry observationRegistry;

    public OutboxEventDispatcher(
            OutboxEventRepository repository,
            RabbitOperations rabbitOperations,
            OutboxRabbitProperties properties,
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry
    ) {
        this.repository = repository;
        this.rabbitOperations = rabbitOperations;
        this.properties = properties;
        this.observationRegistry = observationRegistry;
        this.publishedCounter = Counter.builder("iqarena.outbox.published")
                .description("Количество успешно опубликованных outbox-событий")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("iqarena.outbox.retry")
                .description("Количество временных ошибок публикации outbox-событий")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("iqarena.outbox.failed")
                .description("Количество outbox-событий, переведенных в FAILED")
                .register(meterRegistry);
        this.publishTimer = Timer.builder("iqarena.outbox.publish")
                .description("Время публикации outbox-события в RabbitMQ")
                .register(meterRegistry);
        Gauge.builder("iqarena.outbox.pending", repository, repo -> repo.countByStatus(OutboxEventStatus.PENDING))
                .description("Количество pending outbox-событий")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.rabbit.fixed-delay-ms:2000}")
    @Transactional
    public void dispatchPendingEvents() {
        if (!properties.enabled()) {
            return;
        }

        Instant now = Instant.now();
        repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(OutboxEventStatus.PENDING, now)
                .forEach(this::publishOne);
    }

    private void publishOne(OutboxEventEntity event) {
        Observation.createNotStarted("iqarena.outbox.publish", observationRegistry)
                .lowCardinalityKeyValue("event.type", event.getEventType())
                .lowCardinalityKeyValue("aggregate.type", event.getAggregateType())
                .observe(() -> publishOneObserved(event));
    }

    private void publishOneObserved(OutboxEventEntity event) {
        Timer.Sample sample = Timer.start();
        try {
            rabbitOperations.invoke(operations -> {
                operations.convertAndSend(properties.exchange(), routingKey(event), event.getPayload(), message -> {
                    message.getMessageProperties().setMessageId(event.getId().toString());
                    message.getMessageProperties().setType(event.getEventType());
                    message.getMessageProperties().setTimestamp(java.util.Date.from(event.getOccurredAt()));
                    message.getMessageProperties().setHeader("aggregateType", event.getAggregateType());
                    message.getMessageProperties().setHeader("aggregateId", event.getAggregateId().toString());
                    return message;
                });
                operations.waitForConfirmsOrDie(properties.confirmTimeoutMs());
                return null;
            });

            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            repository.save(event);
            publishedCounter.increment();
        } catch (RuntimeException ex) {
            markFailedAttempt(event, ex);
        } finally {
            sample.stop(publishTimer);
        }
    }

    private void markFailedAttempt(OutboxEventEntity event, RuntimeException ex) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(trimError(ex));

        if (attempts >= properties.maxAttempts()) {
            event.setStatus(OutboxEventStatus.FAILED);
            failedCounter.increment();
            log.warn("Outbox event {} marked FAILED after {} attempts", event.getId(), attempts);
        } else {
            event.setNextAttemptAt(Instant.now().plusSeconds((long) properties.retryBackoffSeconds() * attempts));
            retryCounter.increment();
            log.debug("Outbox event {} publish failed, next attempt at {}", event.getId(), event.getNextAttemptAt());
        }

        repository.save(event);
    }

    private String routingKey(OutboxEventEntity event) {
        return properties.routingKeyPrefix() + "." + event.getEventType().toLowerCase(Locale.ROOT);
    }

    private String trimError(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
        return message.length() > 1_000 ? message.substring(0, 1_000) : message;
    }
}
