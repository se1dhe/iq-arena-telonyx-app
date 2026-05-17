package com.se1dhe.iqarena.telegram;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.repo.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

// Создает или обновляет игрока из Telegram Web App initData.
@Service
@RequiredArgsConstructor
public class TelegramPlayerService {
    private final PlayerRepository playerRepository;

    @Transactional
    public Player upsertFromTelegram(VerifiedTelegramUser user, String referralCode) {
        Player player = playerRepository.findByTelegramUserId(user.id()).orElseGet(Player::new);

        if (player.getId() == null) {
            player.setTelegramUserId(user.id());
            player.setHandle(buildHandle(user));
            player.setDisplayName(buildDisplayName(user));
            player.setReferralCode("IQ" + user.id());

            if (referralCode != null && !referralCode.isBlank()) {
                playerRepository.findByReferralCode(referralCode)
                        .ifPresent(inviter -> player.setReferredByPlayerId(inviter.getId()));
            }
        }

        player.setTelegramUsername(user.username());
        player.setTelegramFirstName(user.firstName());
        player.setTelegramLastName(user.lastName());
        player.setTelegramPhotoUrl(user.photoUrl());
        if (user.languageCode() != null && !user.languageCode().isBlank()) {
            player.setLocale(user.languageCode());
        }
        player.touch();

        return playerRepository.save(player);
    }

    private String buildHandle(VerifiedTelegramUser user) {
        if (user.username() != null && !user.username().isBlank()) {
            String normalized = user.username().toLowerCase(Locale.ROOT).replace('-', '_');
            return normalized.substring(0, Math.min(normalized.length(), 24));
        }
        return ("tg" + user.id()).substring(0, Math.min(("tg" + user.id()).length(), 24));
    }

    private String buildDisplayName(VerifiedTelegramUser user) {
        String name = user.firstName() != null && !user.firstName().isBlank() ? user.firstName() : user.username();
        if (name == null || name.isBlank()) {
            name = "IQ Player";
        }
        return name.substring(0, Math.min(name.length(), 24));
    }
}
