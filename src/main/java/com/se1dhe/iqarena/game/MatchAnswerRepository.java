package com.se1dhe.iqarena.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MatchAnswerRepository extends JpaRepository<MatchAnswerEntity, UUID> {
    boolean existsByMatchIdAndRoundIndexAndPlayerId(UUID matchId, int roundIndex, UUID playerId);
    List<MatchAnswerEntity> findByMatchIdAndRoundIndex(UUID matchId, int roundIndex);
}
