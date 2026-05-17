package com.se1dhe.iqarena.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MatchRoundRepository extends JpaRepository<MatchRoundEntity, UUID> {
    Optional<MatchRoundEntity> findByMatchIdAndRoundIndex(UUID matchId, int roundIndex);
    List<MatchRoundEntity> findByMatchIdOrderByRoundIndexAsc(UUID matchId);
}
