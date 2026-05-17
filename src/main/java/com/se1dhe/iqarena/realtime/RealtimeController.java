package com.se1dhe.iqarena.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

// Выдача wsTicket для MVP. Позже привяжем к JWT CurrentPlayer.
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/realtime")
public class RealtimeController {
    private final WsTicketService wsTicketService;

    @PostMapping("/session/dev/{playerId}")
    public WsTicketResponse session(@PathVariable UUID playerId) {
        return new WsTicketResponse(wsTicketService.issue(playerId), 60);
    }

    public record WsTicketResponse(String wsTicket, int expiresInSeconds) {}
}
