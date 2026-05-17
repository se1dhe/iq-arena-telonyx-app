package com.se1dhe.iqarena.telegram;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Авторизация Telegram Web App через initData.
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth/telegram")
public class TelegramWebAppAuthController {
    private final TelegramInitDataVerifier verifier;
    private final TelegramPlayerService playerService;
    private final JwtService jwtService;

    @PostMapping("/webapp")
    public TelegramAuthResponse webApp(@Valid @RequestBody TelegramAuthRequest request) {
        VerifiedTelegramUser telegramUser = verifier.verify(request.initData());
        Player player = playerService.upsertFromTelegram(telegramUser, request.referralCode());
        String accessToken = jwtService.issueAccessToken(player.getId());

        return new TelegramAuthResponse(
                accessToken,
                "Bearer",
                player.getId(),
                player.getHandle(),
                player.getDisplayName(),
                player.getAvatarId(),
                player.getReferralCode()
        );
    }

    public record TelegramAuthRequest(
            @NotBlank String initData,
            String referralCode
    ) {}

    public record TelegramAuthResponse(
            String accessToken,
            String tokenType,
            UUID playerId,
            String handle,
            String displayName,
            String avatarId,
            String referralCode
    ) {}
}
