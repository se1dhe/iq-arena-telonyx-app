package com.se1dhe.iqarena.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

// Игрок IQ Arena.
@Getter
@Setter
@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 24)
    private String handle;

    @Column(nullable = false, length = 24)
    private String displayName;

    @Column(nullable = false, length = 64)
    private String avatarId = "avatar_01";

    @Column(nullable = false, length = 16)
    private String locale = "ru-RU";

    @Column(nullable = false, length = 32)
    private String status = "active";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
