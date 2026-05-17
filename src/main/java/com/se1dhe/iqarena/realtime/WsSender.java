package com.se1dhe.iqarena.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

// Отправка server -> client WebSocket событий.
@Component
@RequiredArgsConstructor
public class WsSender {
    private final ObjectMapper objectMapper;
    private final WsRegistry registry;
    private final AtomicLong seq = new AtomicLong(1);

    public void sendToPlayer(UUID playerId, String type, Object payload) {
        registry.session(playerId).ifPresent(session -> send(session, type, payload));
    }

    public void send(WebSocketSession session, String type, Object payload) {
        try {
            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "id", UUID.randomUUID().toString(),
                    "seq", seq.getAndIncrement(),
                    "ts", Instant.now().toString(),
                    "payload", payload
            ))));
        } catch (Exception ignored) {
            // В MVP не валим матч из-за оборванной сессии.
        }
    }
}
