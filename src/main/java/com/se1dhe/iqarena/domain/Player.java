package com.se1dhe.iqarena.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Игрок IQ Arena. MVP теперь Telegram-first: основной внешний идентификатор — telegramUserId.
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

    @Column(unique = true)
    private Long telegramUserId;

    @Column(length = 64)
    private String telegramUsername;

    @Column(length = 128)
    private String telegramFirstName;

    @Column(length = 128)
    private String telegramLastName;

    @Column(columnDefinition = "text")
    private String telegramPhotoUrl;

    @Column(unique = true, length = 32)
    private String referralCode;

    private UUID referredByPlayerId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
