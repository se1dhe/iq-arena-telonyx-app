package com.se1dhe.iqarena.matchmaking;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Метрики очереди позволяют контролировать KPI matchmaking latency.
@Component
public class MatchmakingMetrics {
    private final Timer humanPairWaitTimer;
    private final Timer botPairWaitTimer;

    public MatchmakingMetrics(MeterRegistry meterRegistry, MatchmakingQueue queue) {
        Gauge.builder("iqarena.matchmaking.queue.size", queue, MatchmakingQueue::size)
                .description("Текущее количество игроков в Redis matchmaking queue")
                .register(meterRegistry);
        this.humanPairWaitTimer = Timer.builder("iqarena.matchmaking.wait")
                .tag("matchType", "human")
                .description("Время ожидания игрока до human-vs-human матча")
                .register(meterRegistry);
        this.botPairWaitTimer = Timer.builder("iqarena.matchmaking.wait")
                .tag("matchType", "bot")
                .description("Время ожидания игрока до bot fallback матча")
                .register(meterRegistry);
    }

    public void recordHumanPair(MatchmakingPair pair) {
        humanPairWaitTimer.record(Duration.ofMillis(pair.playerOneWaitMs()));
        humanPairWaitTimer.record(Duration.ofMillis(pair.playerTwoWaitMs()));
    }

    public void recordBotPair(long waitMs) {
        if (waitMs >= 0) {
            botPairWaitTimer.record(Duration.ofMillis(waitMs));
        }
    }
}
