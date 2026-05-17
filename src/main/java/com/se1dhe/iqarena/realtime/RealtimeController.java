package com.se1dhe.iqarena.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

// Выдача одноразового wsTicket для подключения к /ws.
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/realtime")
public class RealtimeController {
    private final WsTicketService wsTicketService;

    @Value("${app.ws-ticket.ttl-seconds:60}")
    private int ttlSeconds;

    @PostMapping("/session")
    public WsTicketResponse session(Authentication authentication) {
        UUID playerId = (UUID) authentication.getPrincipal();
        return new WsTicketResponse(wsTicketService.issue(playerId), ttlSeconds);
    }

    @PostMapping("/session/dev/{playerId}")
    public WsTicketResponse session(@PathVariable UUID playerId) {
        return new WsTicketResponse(wsTicketService.issue(playerId), ttlSeconds);
    }

    public record WsTicketResponse(String wsTicket, int expiresInSeconds) {}
}
