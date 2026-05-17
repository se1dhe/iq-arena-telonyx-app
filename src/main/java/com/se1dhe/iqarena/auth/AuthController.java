package com.se1dhe.iqarena.auth;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.repo.PlayerRepository;
import com.se1dhe.iqarena.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;

// Dev auth. Apple Sign In добавим следующим этапом.
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
public class AuthController {
    private final PlayerRepository playerRepository;
    private final JwtService jwtService;

    @PostMapping("/dev/login")
    public AuthResponse devLogin(@Valid @RequestBody DevLoginRequest request) {
        String handle = request.handle().toLowerCase(Locale.ROOT);

        Player player = playerRepository.findByHandle(handle).orElseGet(() -> {
            Player p = new Player();
            p.setHandle(handle);
            p.setDisplayName(request.displayName());
            return playerRepository.save(p);
        });

        return new AuthResponse(jwtService.issueAccessToken(player.getId()), "Bearer",
                player.getId().toString(), player.getHandle(), player.getDisplayName());
    }

    public record DevLoginRequest(
            @Pattern(regexp = "^[a-zA-Z0-9_]{3,24}$") String handle,
            @Size(min = 3, max = 24) String displayName
    ) {}

    public record AuthResponse(String accessToken, String tokenType, String playerId, String handle, String displayName) {}
}
