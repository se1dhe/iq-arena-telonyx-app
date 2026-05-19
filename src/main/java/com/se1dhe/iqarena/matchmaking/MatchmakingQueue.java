package com.se1dhe.iqarena.matchmaking;

import java.util.Optional;
import java.util.UUID;

public interface MatchmakingQueue {
    long UNKNOWN_WAIT_MS = -1;

    void join(UUID playerId, String category);

    void leave(UUID playerId);

    Optional<MatchmakingPair> pollPair();

    Optional<QueuedPlayer> removeIfQueued(UUID playerId);

    long size();
}
