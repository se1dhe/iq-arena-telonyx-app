package com.se1dhe.iqarena.game;

import com.se1dhe.iqarena.domain.Player;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Ответ игрока на вопрос.
@Getter
@Setter
@Entity
@Table(name = "match_answers")
public class MatchAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private MatchEntity match;

    private int roundIndex;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    private Integer selectedIndex;
    private boolean correct;
    private Integer responseMs;
    private int points;
    private Instant acceptedAt = Instant.now();
}
