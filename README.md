# IQ Arena Telonyx App — Telegram PvP Quiz

IQ Arena — PvP-викторина 1v1, где вся игра проходит внутри Telegram: через Telegram Bot и Telegram Web App.

## Новый продуктовый фокус

На текущем этапе **не делаем отдельную iOS/macOS игру**. Основная платформа MVP:

- Telegram Bot как точка входа
- Telegram Web App как основной игровой интерфейс
- backend на Java 21 + Spring Boot
- realtime через WebSocket
- PostgreSQL для игроков, вопросов, матчей и рейтинга
- Redis для очередей, presence, matchmaking и временных состояний

## Почему Telegram-first

- быстрее вывести MVP
- не нужен App Store review на первом этапе
- проще тестировать на реальных пользователях
- проще делать вирусность через инвайты, группы, каналы и челленджи
- можно позже вынести механику в iOS/macOS клиент, не ломая backend

## Игровой цикл MVP

1. Пользователь открывает бота.
2. Нажимает кнопку `Играть`.
3. Бот открывает Telegram Web App.
4. Web App авторизуется через Telegram initData.
5. Игрок нажимает `Найти соперника`.
6. Backend ставит игрока в matchmaking queue.
7. Когда найден второй игрок, обоим открывается матч.
8. Матч идет на 5 вопросов.
9. На каждый вопрос дается 10 секунд.
10. Backend считает очки, победителя и рейтинг.
11. Результат показывается в Web App и может быть отправлен в Telegram-чат.

## Стек

- Java 21
- Spring Boot 3.5
- PostgreSQL
- Redis
- Flyway
- JWT / Telegram initData auth
- Raw WebSocket `/ws`
- Telegram Bot API
- Telegram Web App
- Glicko-like рейтинг для MVP

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

WebSocket ticket для dev-режима:

```bash
curl -X POST http://localhost:8080/v1/realtime/session/dev/<PLAYER_ID>
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
- клиент никогда не получает правильный ответ до reveal-фазы

## Ближайшие задачи

1. Добавить полноценную проверку Telegram Web App initData.
2. Добавить Telegram Bot polling/webhook слой.
3. Сделать кнопку `Играть` с `web_app` URL.
4. Реализовать полноценный matchmaking на Redis.
5. Реализовать match engine: 5 раундов, answers, reveal, result.
6. Добавить frontend Telegram Web App.

## Документация и контракты

- `docs/README.md`
- `docs/game/ranked-duel-rules.md`
- `docs/game/scoring-and-tiebreaks.md`
- `contracts/websocket-events.md`
- `contracts/openapi.yaml`
- `contracts/asyncapi.yaml`

## Railway deploy

Railway deploys this app from GitHub using `railway.json`.

Required service variables:

- `APP_JWT_SECRET`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_BOT_USERNAME`
- `TELEGRAM_WEBAPP_URL`
- `APP_PUBLIC_BASE_URL`
- `APP_WEBAPP_URL`
