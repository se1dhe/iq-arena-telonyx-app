package com.se1dhe.iqarena.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

// Рейтинговое состояние игрока.
@Getter
@Setter
@Entity
@Table(name = "ratings")
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    private String mode = "ranked_duel";
    private String seasonId = "season_0";
    private double rating = 1500;
    private double rd = 350;
    private double sigma = 0.06;
    private int gamesPlayed = 0;
    private Instant lastRatedAt = Instant.now();
}
