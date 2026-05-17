package com.se1dhe.iqarena.profile;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.repo.PlayerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/me")
public class MeController {
    private final PlayerRepository playerRepository;

    @GetMapping
    public ProfileController.PlayerDto me(Authentication authentication) {
        return ProfileController.PlayerDto.from(load(authentication));
    }

    @PatchMapping
    public ProfileController.PlayerDto update(Authentication authentication, @Valid @RequestBody UpdateMeRequest request) {
        Player player = load(authentication);
        if (request.displayName() != null) {
            player.setDisplayName(request.displayName());
        }
        if (request.avatarId() != null) {
            player.setAvatarId(request.avatarId());
        }
        if (request.locale() != null) {
            player.setLocale(request.locale());
        }
        player.touch();
        return ProfileController.PlayerDto.from(playerRepository.save(player));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(Authentication authentication) {
        Player player = load(authentication);
        player.setStatus("deleted");
        player.setDisplayName("Deleted Player");
        player.touch();
        playerRepository.save(player);
        return ResponseEntity.noContent().build();
    }

    private Player load(Authentication authentication) {
        UUID playerId = (UUID) authentication.getPrincipal();
        return playerRepository.findById(playerId).orElseThrow();
    }

    public record UpdateMeRequest(
            @Size(min = 3, max = 24) String displayName,
            @Pattern(regexp = "^avatar_[a-zA-Z0-9_]{1,32}$") String avatarId,
            @Size(min = 2, max = 16) String locale
    ) {}
}

