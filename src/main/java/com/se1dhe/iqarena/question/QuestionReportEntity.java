package com.se1dhe.iqarena.question;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.domain.Question;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "question_reports")
public class QuestionReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(nullable = false, length = 64)
    private String reason;

    private UUID matchId;
    private Integer roundIndex;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false, length = 32)
    private String status = "open";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}

