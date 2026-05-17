package com.se1dhe.iqarena.game;

import com.se1dhe.iqarena.domain.Question;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Раунд матча.
@Getter
@Setter
@Entity
@Table(name = "match_rounds")
public class MatchRoundEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private MatchEntity match;

    private int roundIndex;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    private Instant openedAt;
    private Instant deadlineAt;
    private int correctIndex;
}
