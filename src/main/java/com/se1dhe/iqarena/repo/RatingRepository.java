package com.se1dhe.iqarena.repo;

import com.se1dhe.iqarena.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByPlayerIdAndModeAndSeasonId(UUID playerId, String mode, String seasonId);
    List<Rating> findTop100ByModeAndSeasonIdOrderByRatingDesc(String mode, String seasonId);
}
