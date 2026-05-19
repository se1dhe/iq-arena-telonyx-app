package com.se1dhe.iqarena.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Публикует события активности игрока для retention и analytics агентов.
@Service
@RequiredArgsConstructor
public class PlayerActivityEvents {
    private final DomainEventPublisher publisher;

    public void authenticated(UUID playerId, String handle, String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", playerId.toString());
        payload.put("handle", handle);
        payload.put("source", source);
        payload.put("authenticatedAt", Instant.now().toString());
        publisher.publish("player", playerId, "PLAYER_AUTHENTICATED", payload);
    }
}
