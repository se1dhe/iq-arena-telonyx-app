package com.se1dhe.iqarena.matchmaking;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.game.GameService;
import com.se1dhe.iqarena.repo.PlayerRepository;
import com.se1dhe.iqarena.realtime.WsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

// Оркестратор матчмейкинга: состояние очереди хранится в Redis.
@Service
@RequiredArgsConstructor
public class MatchmakingService {
    private static final String BOT_HANDLE = "arena_bot";
    private static final String BOT_DISPLAY_NAME = "IQ Arena Bot";

    private final MatchmakingQueue queue;
    private final MatchmakingRedisLock lock;
    private final MatchmakingMetrics metrics;
    private final GameService gameService;
    private final WsSender wsSender;
    private final PlayerRepository playerRepository;
    private final TaskScheduler taskScheduler;

    @Value("${app.matchmaking.bot-wait-seconds:4}")
    private long botWaitSeconds;

    public void join(UUID playerId, String category) {
        String normalizedCategory = normalizeCategory(category);
        queue.join(playerId, normalizedCategory);

        wsSender.sendToPlayer(playerId, "queue.status", Map.of(
                "status", "searching",
                "joinedAt", Instant.now().toString(),
                "category", normalizedCategory
        ));

        tryPair();
        taskScheduler.schedule(() -> matchWithBotIfStillQueued(playerId), Instant.now().plusSeconds(botWaitSeconds));
    }

    public void leave(UUID playerId) {
        queue.leave(playerId);
        wsSender.sendToPlayer(playerId, "queue.status", Map.of("status", "idle"));
    }

    private void tryPair() {
        lock.withLock(() -> queue.pollPair()).value().ifPresent(pair -> {
            metrics.recordHumanPair(pair);
            gameService.createMatch(pair.playerOneId(), pair.playerTwoId(), pair.category());
        });
    }

    private void matchWithBotIfStillQueued(UUID playerId) {
        LockResult<QueuedPlayer> result = lock.withLock(() -> queue.removeIfQueued(playerId));
        if (!result.acquired()) {
            taskScheduler.schedule(() -> matchWithBotIfStillQueued(playerId), Instant.now().plusSeconds(1));
            return;
        }

        result.value().ifPresent(queuedPlayer -> {
            metrics.recordBotPair(queuedPlayer.waitMs());
            Player bot = findOrCreateBot();
            gameService.createMatch(playerId, bot.getId(), queuedPlayer.category());
        });
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "mixed";
        }
        return switch (category) {
            case "history", "geography", "science", "art", "logic" -> category;
            default -> "mixed";
        };
    }

    private Player findOrCreateBot() {
        return playerRepository.findByHandle(BOT_HANDLE).orElseGet(() -> {
            Player bot = new Player();
            bot.setHandle(BOT_HANDLE);
            bot.setDisplayName(BOT_DISPLAY_NAME);
            bot.setAvatarId("avatar_bot");
            bot.setLocale("ru-RU");
            bot.setStatus("system");
            return playerRepository.save(bot);
        });
    }
}
