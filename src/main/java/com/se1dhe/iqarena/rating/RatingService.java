package com.se1dhe.iqarena.rating;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.domain.Rating;
import com.se1dhe.iqarena.game.MatchEntity;
import com.se1dhe.iqarena.repo.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RatingService {
    private static final String MODE = "ranked_duel";
    private static final String SEASON_ID = "season_0";

    private final RatingRepository ratingRepository;
    private final Glicko2Calculator calculator;

    public List<Map<String, Object>> updateAfterMatch(MatchEntity match) {
        Rating one = findOrCreate(match.getPlayerOne());
        Rating two = findOrCreate(match.getPlayerTwo());

        double oneOld = one.getRating();
        double twoOld = two.getRating();
        double oneScore = scoreFor(match.getPlayerOneScore(), match.getPlayerTwoScore());
        double twoScore = scoreFor(match.getPlayerTwoScore(), match.getPlayerOneScore());

        one.setRating(calculator.update(oneOld, twoOld, oneScore).rating());
        two.setRating(calculator.update(twoOld, oneOld, twoScore).rating());
        one.setGamesPlayed(one.getGamesPlayed() + 1);
        two.setGamesPlayed(two.getGamesPlayed() + 1);
        one.setLastRatedAt(Instant.now());
        two.setLastRatedAt(Instant.now());

        ratingRepository.save(one);
        ratingRepository.save(two);

        return List.of(
                payload(one.getPlayer(), oneOld, one),
                payload(two.getPlayer(), twoOld, two)
        );
    }

    private Rating findOrCreate(Player player) {
        return ratingRepository.findByPlayerIdAndModeAndSeasonId(player.getId(), MODE, SEASON_ID)
                .orElseGet(() -> {
                    Rating rating = new Rating();
                    rating.setPlayer(player);
                    rating.setMode(MODE);
                    rating.setSeasonId(SEASON_ID);
                    return rating;
                });
    }

    private double scoreFor(int ownScore, int opponentScore) {
        if (ownScore > opponentScore) {
            return 1.0;
        }
        if (ownScore < opponentScore) {
            return 0.0;
        }
        return 0.5;
    }

    private Map<String, Object> payload(Player player, double oldRating, Rating rating) {
        int oldVisible = (int) Math.round(oldRating);
        int newVisible = (int) Math.round(rating.getRating());
        return Map.of(
                "playerId", player.getId().toString(),
                "mode", rating.getMode(),
                "seasonId", rating.getSeasonId(),
                "oldRating", oldVisible,
                "newRating", newVisible,
                "delta", newVisible - oldVisible,
                "gamesPlayed", rating.getGamesPlayed()
        );
    }
}

