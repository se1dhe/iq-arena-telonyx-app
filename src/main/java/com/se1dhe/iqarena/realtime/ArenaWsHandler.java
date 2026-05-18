package com.se1dhe.iqarena.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1dhe.iqarena.game.GameService;
import com.se1dhe.iqarena.matchmaking.MatchmakingService;
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
    private final WsSender wsSender;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID playerId = ticketService.consume(extractTicket(session));
        registry.register(playerId, session);
        wsSender.send(session, "welcome", Map.of("playerId", playerId.toString(), "serverTime", Instant.now().toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID playerId = registry.playerId(session).orElseThrow();
        var root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText();
        var payload = root.path("payload");

        if ("ping".equals(type)) {
            wsSender.send(session, "pong", Map.of("serverTime", Instant.now().toString()));
            return;
        }

        if ("queue.join".equals(type)) {
            matchmakingService.join(playerId, payload.path("category").asText("mixed"));
            return;
        }

        if ("queue.leave".equals(type)) {
            matchmakingService.leave(playerId);
            return;
        }

        if ("round.answer".equals(type)) {
            UUID matchId = UUID.fromString(payload.path("matchId").asText());
            int round = payload.path("round").asInt();
            int selectedIndex = payload.path("selectedIndex").asInt();
            gameService.answer(playerId, matchId, round, selectedIndex);
            return;
        }

        wsSender.send(session, "error", Map.of("code", "UNKNOWN_EVENT", "message", "Неизвестное событие: " + type));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.playerId(session).ifPresent(matchmakingService::leave);
        registry.unregister(session);
    }

    private String extractTicket(WebSocketSession session) {
        String query = session.getUri() == null ? "" : session.getUri().getQuery();
        if (query == null) {
            throw new IllegalArgumentException("ticket missing");
        }
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "ticket".equals(kv[0])) {
                return kv[1];
            }
        }
        throw new IllegalArgumentException("ticket missing");
    }
}
