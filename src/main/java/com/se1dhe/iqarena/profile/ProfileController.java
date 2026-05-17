package com.se1dhe.iqarena.profile;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.repo.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

// API профиля игрока.
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/players")
public class ProfileController {
    private final PlayerRepository playerRepository;

    @GetMapping("/{id}")
    public PlayerDto get(@PathVariable UUID id) {
        Player p = playerRepository.findById(id).orElseThrow();
        return PlayerDto.from(p);
    }

    public record PlayerDto(UUID id, String handle, String displayName, String avatarId, String locale, String status) {
        static PlayerDto from(Player p) {
            return new PlayerDto(p.getId(), p.getHandle(), p.getDisplayName(), p.getAvatarId(), p.getLocale(), p.getStatus());
        }
    }
}
