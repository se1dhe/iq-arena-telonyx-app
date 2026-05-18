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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

// In-memory matchmaking для MVP. Позже переносим состояние в Redis.
@Service
@RequiredArgsConstructor
public class MatchmakingService {
    private static final String BOT_HANDLE = "arena_bot";
    private static final String BOT_DISPLAY_NAME = "IQ Arena Bot";

    private final Queue<UUID> queue = new ConcurrentLinkedQueue<>();
    private final Set<UUID> queuedPlayers = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, String> preferredCategories = new ConcurrentHashMap<>();
    private final GameService gameService;
    private final WsSender wsSender;
    private final PlayerRepository playerRepository;
    private final TaskScheduler taskScheduler;

    @Value("${app.matchmaking.bot-wait-seconds:4}")
    private long botWaitSeconds;

    public void join(UUID playerId, String category) {
        preferredCategories.put(playerId, normalizeCategory(category));
        if (queuedPlayers.add(playerId)) {
            queue.add(playerId);
        }

        wsSender.sendToPlayer(playerId, "queue.status", Map.of(
                "status", "searching",
                "joinedAt", Instant.now().toString(),
                "category", preferredCategories.get(playerId)
        ));

        tryPair();
        taskScheduler.schedule(() -> matchWithBotIfStillQueued(playerId), Instant.now().plusSeconds(botWaitSeconds));
    }

    public void leave(UUID playerId) {
        queuedPlayers.remove(playerId);
        queue.remove(playerId);
        preferredCategories.remove(playerId);
        wsSender.sendToPlayer(playerId, "queue.status", Map.of("status", "idle"));
    }

    private synchronized void tryPair() {
        while (queue.size() >= 2) {
            UUID one = queue.poll();
            UUID two = queue.poll();

            if (one == null || two == null || one.equals(two)) {
                return;
            }

            queuedPlayers.remove(one);
            queuedPlayers.remove(two);
            gameService.createMatch(one, two, matchCategory(one, two));
            preferredCategories.remove(one);
            preferredCategories.remove(two);
        }
    }

    private synchronized void matchWithBotIfStillQueued(UUID playerId) {
        if (!queuedPlayers.remove(playerId)) {
            return;
        }

        queue.remove(playerId);
        Player bot = findOrCreateBot();
        gameService.createMatch(playerId, bot.getId(), preferredCategories.remove(playerId));
    }

    private String matchCategory(UUID one, UUID two) {
        String first = preferredCategories.getOrDefault(one, "mixed");
        String second = preferredCategories.getOrDefault(two, "mixed");
        if (!"mixed".equals(first) && first.equals(second)) {
            return first;
        }
        return "mixed";
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
