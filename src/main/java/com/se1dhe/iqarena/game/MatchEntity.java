package com.se1dhe.iqarena.game;

import com.se1dhe.iqarena.domain.Player;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Заголовок PvP-матча.
@Getter
@Setter
@Entity
@Table(name = "matches")
public class MatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private MatchState state;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_one_id")
    private Player playerOne;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_two_id")
    private Player playerTwo;

    @ManyToOne
    @JoinColumn(name = "winner_player_id")
    private Player winnerPlayer;

    private int playerOneScore;
    private int playerTwoScore;

    private Instant createdAt = Instant.now();
    private Instant startedAt;
    private Instant completedAt;
}
