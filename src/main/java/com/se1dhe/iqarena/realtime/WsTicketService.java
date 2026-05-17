package com.se1dhe.iqarena.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Одноразовые ws tickets. В проде переносим в Redis.
@Service
public class WsTicketService {
    private final long ttlSeconds;
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public WsTicketService(@Value("${app.ws-ticket.ttl-seconds}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(UUID playerId) {
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new Ticket(playerId, Instant.now().plusSeconds(ttlSeconds)));
        return ticket;
    }

    public UUID consume(String ticket) {
        Ticket value = tickets.remove(ticket);
        if (value == null || value.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Некорректный wsTicket");
        }
        return value.playerId();
    }

    private record Ticket(UUID playerId, Instant expiresAt) {}
}
