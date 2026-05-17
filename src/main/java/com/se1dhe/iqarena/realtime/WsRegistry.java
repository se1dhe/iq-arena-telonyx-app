package com.se1dhe.iqarena.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Реестр активных WS-сессий.
@Component
public class WsRegistry {
    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> playerBySession = new ConcurrentHashMap<>();

    public void register(UUID playerId, WebSocketSession session) {
        sessions.put(playerId, session);
        playerBySession.put(session.getId(), playerId);
    }

    public void unregister(WebSocketSession session) {
        UUID playerId = playerBySession.remove(session.getId());
        if (playerId != null) {
            sessions.remove(playerId);
        }
    }

    public Optional<UUID> playerId(WebSocketSession session) {
        return Optional.ofNullable(playerBySession.get(session.getId()));
    }

    public Optional<WebSocketSession> session(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public boolean isOnline(UUID playerId) {
        return session(playerId).map(WebSocketSession::isOpen).orElse(false);
    }
}
