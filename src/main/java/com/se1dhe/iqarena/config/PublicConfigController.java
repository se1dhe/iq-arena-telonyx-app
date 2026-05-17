package com.se1dhe.iqarena.config;

import org.springframework.web.bind.annotation.*;
import java.util.*;

// Публичная конфигурация для клиента.
@RestController
@RequestMapping("/v1/config")
public class PublicConfigController {
    @GetMapping("/public")
    public Map<String, Object> config() {
        return Map.of(
                "game", Map.of("rounds", 5, "roundSeconds", 10, "mode", "ranked_duel"),
                "categories", List.of("history", "sports", "geography", "movies", "games", "it", "music", "science")
        );
    }
}
