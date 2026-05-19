package com.se1dhe.iqarena.events;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitOperations.OperationsCallback;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxEventDispatcherTest {
    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final RabbitOperations rabbitOperations = mock(RabbitOperations.class);
    private final OutboxRabbitProperties properties = new OutboxRabbitProperties(
            true,
            "iq-arena.domain-events",
            "iq-arena",
            2,
            5,
            5_000
    );
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final OutboxEventDispatcher dispatcher = new OutboxEventDispatcher(
            repository,
            rabbitOperations,
            properties,
            meterRegistry,
            ObservationRegistry.NOOP
    );

    @Test
    void successfulPublishMarksEventAsPublished() {
        OutboxEventEntity event = eventWithAttempts(0);
        when(repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(eq(OutboxEventStatus.PENDING), any()))
                .thenReturn(List.of(event));
        invokeRabbitCallback();

        dispatcher.dispatchPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
        verify(rabbitOperations).convertAndSend(
                eq("iq-arena.domain-events"),
                eq("iq-arena.match_created"),
                eq(event.getPayload()),
                any(MessagePostProcessor.class)
        );
        verify(rabbitOperations).waitForConfirmsOrDie(5_000);
        verify(repository, atLeastOnce()).save(event);
        assertThat(meterRegistry.counter("iqarena.outbox.published").count()).isEqualTo(1);
    }

    @Test
    void failedPublishSchedulesRetryBeforeMaxAttempts() {
        OutboxEventEntity event = eventWithAttempts(0);
        when(repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(eq(OutboxEventStatus.PENDING), any()))
                .thenReturn(List.of(event));
        invokeRabbitCallback();
        doThrow(new IllegalStateException("rabbit is down"))
                .when(rabbitOperations)
                .convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));

        dispatcher.dispatchPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(event.getLastError()).contains("rabbit is down");
        verify(repository, atLeastOnce()).save(event);
        assertThat(meterRegistry.counter("iqarena.outbox.retry").count()).isEqualTo(1);
    }

    @Test
    void failedPublishMarksEventAsFailedAtMaxAttempts() {
        OutboxEventEntity event = eventWithAttempts(1);
        when(repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(eq(OutboxEventStatus.PENDING), any()))
                .thenReturn(List.of(event));
        invokeRabbitCallback();
        doThrow(new IllegalStateException("rabbit is down"))
                .when(rabbitOperations)
                .convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));

        dispatcher.dispatchPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getLastError()).contains("rabbit is down");
        verify(repository, atLeastOnce()).save(event);
        assertThat(meterRegistry.counter("iqarena.outbox.failed").count()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private void invokeRabbitCallback() {
        when(rabbitOperations.invoke(any(OperationsCallback.class))).thenAnswer(invocation -> {
            OperationsCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(rabbitOperations);
        });
    }

    private OutboxEventEntity eventWithAttempts(int attempts) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateType("match");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("MATCH_CREATED");
        event.setPayload(Map.of("matchId", event.getAggregateId().toString()));
        event.setStatus(OutboxEventStatus.PENDING);
        event.setAttempts(attempts);
        event.setOccurredAt(Instant.now().minusSeconds(1));
        event.setNextAttemptAt(Instant.now().minusSeconds(1));
        return event;
    }
}
