package com.se1dhe.iqarena.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

// Конфигурация /ws endpoint.
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WsConfig implements WebSocketConfigurer {
    private final ArenaWsHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").setAllowedOriginPatterns("*");
    }
}
