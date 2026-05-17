package com.se1dhe.iqarena.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.time.Instant;
import java.util.*;

// Raw WebSocket handler для MVP.
@Component
@RequiredArgsConstructor
public class ArenaWsHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final WsTicketService ticketService;
    private final WsRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID playerId = ticketService.consume(extractTicket(session));
        registry.register(playerId, session);
        send(session, "welcome", Map.of("playerId", playerId.toString(), "serverTime", Instant.now().toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        var root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText();

        if ("ping".equals(type)) {
            send(session, "pong", Map.of("serverTime", Instant.now().toString()));
            return;
        }

        if ("queue.join".equals(type)) {
            send(session, "queue.status", Map.of("status", "searching"));
            send(session, "mvp.note", Map.of("message", "Matchmaking service подключается следующим коммитом."));
            return;
        }

        send(session, "error", Map.of("code", "UNKNOWN_EVENT", "message", "Неизвестное событие: " + type));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(session);
    }

    private void send(WebSocketSession session, String type, Object payload) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", type,
                "id", UUID.randomUUID().toString(),
                "ts", Instant.now().toString(),
                "payload", payload
        ))));
    }

    private String extractTicket(WebSocketSession session) {
        String query = session.getUri() == null ? "" : session.getUri().getQuery();
        if (query == null) throw new IllegalArgumentException("ticket missing");
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "ticket".equals(kv[0])) return kv[1];
        }
        throw new IllegalArgumentException("ticket missing");
    }
}
