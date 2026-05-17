package com.se1dhe.iqarena.matchmaking;

import com.se1dhe.iqarena.game.GameService;
import com.se1dhe.iqarena.realtime.WsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// In-memory matchmaking для MVP. Позже переносим состояние в Redis.
@Service
@RequiredArgsConstructor
public class MatchmakingService {
    private final Queue<UUID> queue = new ConcurrentLinkedQueue<>();
    private final Set<UUID> queuedPlayers = Collections.synchronizedSet(new HashSet<>());
    private final GameService gameService;
    private final WsSender wsSender;

    public void join(UUID playerId) {
        if (queuedPlayers.add(playerId)) {
            queue.add(playerId);
        }

        wsSender.sendToPlayer(playerId, "queue.status", Map.of(
                "status", "searching",
                "joinedAt", Instant.now().toString()
        ));

        tryPair();
    }

    public void leave(UUID playerId) {
        queuedPlayers.remove(playerId);
        queue.remove(playerId);
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
            gameService.createMatch(one, two);
        }
    }
}
