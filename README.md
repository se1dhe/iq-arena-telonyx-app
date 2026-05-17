# IQ Arena Telonyx App — Backend Bootstrap

Realtime PvP quiz backend для iOS/macOS игры IQ Arena.

## Стек

- Java 21
- Spring Boot 3.5
- PostgreSQL
- Redis
- Flyway
- JWT
- Raw WebSocket `/ws`
- Glicko-2 рейтинг
- Server-authoritative match engine

## Быстрый старт

```bash
docker compose up -d postgres redis
mvn spring-boot:run
```

Healthcheck:

```bash
curl http://localhost:8080/actuator/health
```

Dev login:

```bash
curl -X POST http://localhost:8080/v1/auth/dev/login \
  -H "Content-Type: application/json" \
  -d '{"handle":"neofox","displayName":"NeoFox"}'
```

WebSocket ticket:

```bash
curl -X POST http://localhost:8080/v1/realtime/session \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Connect:

```text
ws://localhost:8080/ws?ticket=<WS_TICKET>
```

## MVP правила

- 1v1 Ranked Duel
- 5 вопросов
- 10 секунд на вопрос
- сервер контролирует таймер, ответы, очки и рейтинг
- неправильный ответ/таймаут = 0
- правильный ответ = 100 + бонус скорости
