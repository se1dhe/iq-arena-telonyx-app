package com.se1dhe.iqarena.rating;

import com.se1dhe.iqarena.repo.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Leaderboard API.
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/leaderboards")
public class LeaderboardController {
    private final RatingRepository ratingRepository;

    @GetMapping("/global")
    public List<Row> global() {
        AtomicInteger rank = new AtomicInteger(1);
        return ratingRepository.findTop100ByModeAndSeasonIdOrderByRatingDesc("ranked_duel", "season_0")
                .stream()
                .map(r -> new Row(rank.getAndIncrement(), r.getPlayer().getDisplayName(), (int)Math.round(r.getRating()), r.getGamesPlayed()))
                .toList();
    }

    public record Row(int rank, String displayName, int iqRating, int gamesPlayed) {}
}
