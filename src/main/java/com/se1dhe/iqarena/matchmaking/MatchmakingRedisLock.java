package com.se1dhe.iqarena.matchmaking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

// Distributed lock защищает выбор пары от гонок между несколькими инстансами приложения.
@Component
@RequiredArgsConstructor
public class MatchmakingRedisLock {
    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final MatchmakingRedisProperties properties;

    public <T> LockResult<T> withLock(Supplier<Optional<T>> action) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(properties.lockKey(), token, Duration.ofMillis(properties.lockTtlMs()));
        if (!Boolean.TRUE.equals(acquired)) {
            return LockResult.notAcquired();
        }

        try {
            return LockResult.acquired(action.get());
        } finally {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(properties.lockKey()), token);
        }
    }
}
