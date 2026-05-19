package com.se1dhe.iqarena.matchmaking;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Ключи Redis для горизонтально масштабируемого матчмейкинга.
@ConfigurationProperties(prefix = "app.matchmaking.redis")
public record MatchmakingRedisProperties(
        String queueKey,
        String categoryKey,
        String lockKey,
        long lockTtlMs,
        long staleAfterSeconds
) {
}
