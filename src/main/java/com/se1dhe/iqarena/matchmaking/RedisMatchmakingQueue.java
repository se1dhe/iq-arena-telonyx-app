package com.se1dhe.iqarena.matchmaking;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Redis-backed очередь матчмейкинга; score хранит время входа игрока в очередь.
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(MatchmakingRedisProperties.class)
public class RedisMatchmakingQueue implements MatchmakingQueue {
    private static final String MIXED_CATEGORY = "mixed";

    private final StringRedisTemplate redisTemplate;
    private final MatchmakingRedisProperties properties;

    @Override
    public void join(UUID playerId, String category) {
        cleanupStalePlayers();
        String playerKey = playerId.toString();
        redisTemplate.opsForHash().put(properties.categoryKey(), playerKey, category);
        redisTemplate.opsForZSet().add(properties.queueKey(), playerKey, Instant.now().toEpochMilli());
    }

    @Override
    public void leave(UUID playerId) {
        String playerKey = playerId.toString();
        redisTemplate.opsForZSet().remove(properties.queueKey(), playerKey);
        redisTemplate.opsForHash().delete(properties.categoryKey(), playerKey);
    }

    @Override
    public Optional<MatchmakingPair> pollPair() {
        cleanupStalePlayers();
        Set<String> candidates = redisTemplate.opsForZSet().range(properties.queueKey(), 0, 1);
        if (candidates == null || candidates.size() < 2) {
            return Optional.empty();
        }

        List<String> playerKeys = candidates.stream().toList();
        String one = playerKeys.get(0);
        String two = playerKeys.get(1);
        if (one.equals(two)) {
            leave(UUID.fromString(one));
            return Optional.empty();
        }

        String category = matchCategory(categoryOf(one), categoryOf(two));
        long now = Instant.now().toEpochMilli();
        long oneWaitMs = waitMs(one, now);
        long twoWaitMs = waitMs(two, now);
        Long removed = redisTemplate.opsForZSet().remove(properties.queueKey(), one, two);
        if (removed == null || removed < 2) {
            return Optional.empty();
        }

        redisTemplate.opsForHash().delete(properties.categoryKey(), one, two);
        return Optional.of(new MatchmakingPair(UUID.fromString(one), UUID.fromString(two), category, oneWaitMs, twoWaitMs));
    }

    @Override
    public Optional<QueuedPlayer> removeIfQueued(UUID playerId) {
        String playerKey = playerId.toString();
        Object category = redisTemplate.opsForHash().get(properties.categoryKey(), playerKey);
        long waitMs = waitMs(playerKey, Instant.now().toEpochMilli());
        Long removed = redisTemplate.opsForZSet().remove(properties.queueKey(), playerKey);
        redisTemplate.opsForHash().delete(properties.categoryKey(), playerKey);
        if (removed == null || removed == 0) {
            return Optional.empty();
        }
        return Optional.of(new QueuedPlayer(category == null ? MIXED_CATEGORY : category.toString(), waitMs));
    }

    @Override
    public long size() {
        Long size = redisTemplate.opsForZSet().size(properties.queueKey());
        return size == null ? 0 : size;
    }

    private void cleanupStalePlayers() {
        long threshold = Instant.now().minusSeconds(properties.staleAfterSeconds()).toEpochMilli();
        Set<String> stalePlayers = redisTemplate.opsForZSet().rangeByScore(properties.queueKey(), 0, threshold);
        if (stalePlayers == null || stalePlayers.isEmpty()) {
            return;
        }

        redisTemplate.opsForZSet().remove(properties.queueKey(), stalePlayers.toArray());
        redisTemplate.opsForHash().delete(properties.categoryKey(), stalePlayers.toArray());
    }

    private String categoryOf(String playerKey) {
        Object value = redisTemplate.opsForHash().get(properties.categoryKey(), playerKey);
        return value == null ? MIXED_CATEGORY : value.toString();
    }

    private long waitMs(String playerKey, long now) {
        Double score = redisTemplate.opsForZSet().score(properties.queueKey(), playerKey);
        return score == null ? 0 : Math.max(0, now - score.longValue());
    }

    private String matchCategory(String first, String second) {
        if (!MIXED_CATEGORY.equals(first) && first.equals(second)) {
            return first;
        }
        return MIXED_CATEGORY;
    }
}
