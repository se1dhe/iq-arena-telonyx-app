package com.se1dhe.iqarena.matchmaking;

import java.util.UUID;

public record MatchmakingPair(
        UUID playerOneId,
        UUID playerTwoId,
        String category,
        long playerOneWaitMs,
        long playerTwoWaitMs
) {
}
