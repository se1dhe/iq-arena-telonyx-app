# IQ Arena implementation plan

## Текущее состояние

- Backend реализован как Spring Boot 3.5 / Java 21 modular monolith на Maven.
- Есть PostgreSQL, Flyway, Redis, JWT, Telegram Web App auth, Telegram Bot, WebSocket `/ws`.
- Игровой MVP уже содержит матчмейкинг, 1v1 match engine, раунды, ответы, скоринг, рейтинг и статический Telegram Mini App.
- Есть OpenAPI/AsyncAPI контракты, ADR и базовые unit/smoke-тесты.

## Инкремент 1: event-driven foundation

Статус: реализовано.

- Добавлен PostgreSQL transactional outbox `event_outbox`.
- Добавлен доменный publisher для записи событий в outbox.
- `GameService` публикует `MATCH_CREATED` и `MATCH_COMPLETED`.
- Auth flow публикует `PLAYER_AUTHENTICATED` для retention/analytics.
- `compose.yaml` расширен RabbitMQ с management UI.
- AsyncAPI описывает канал доменных событий `iq-arena.domain-events`.

## Инкремент 2: outbox dispatcher

Цель: доставлять события из PostgreSQL outbox в RabbitMQ без потерь.

Статус: реализовано.

- Добавлен Spring AMQP dependency.
- Добавлен exchange `iq-arena.domain-events`, routing keys и publisher confirms.
- Реализован scheduled dispatcher с batch processing, retry, backoff и статусами `PUBLISHED` / `FAILED`.
- Добавлены метрики: pending events, publish latency, retry, published, failed events.
- Добавлены unit-тесты dispatcher.

## Инкремент 3: Redis matchmaking

Цель: убрать in-memory очередь, чтобы приложение масштабировалось горизонтально.

Статус: частично реализовано.

- Очередь игроков и категории перенесены в Redis Sorted Set + Hash.
- Добавлен Redis distributed lock для выбора пары и bot fallback.
- Добавлена очистка зависших игроков по `stale-after-seconds`.
- Добавлены Prometheus-метрики queue size и wait time.
- Bot fallback сохранен как отдельная политика ожидания.
- Далее: добавить fairness window по рейтингу.

## Инкремент 4: observability and security hardening

Цель: production-ready эксплуатация.

- Добавить correlation ID filter и JSON structured logs.
- Расширить actuator endpoints для Prometheus.
- Добавить rate limiting на auth/realtime endpoints.
- Добавить request validation и единый формат validation errors.
- Добавить OpenTelemetry tracing для REST, WebSocket и outbox dispatcher.

Статус: частично реализовано.

- Добавлен `X-Correlation-Id` filter, MDC и correlationId в JSON-ошибках.
- Подключен Prometheus registry и `/actuator/prometheus`.
- Добавлен Redis-backed rate limiting для auth/realtime endpoint.
- Добавлен единый JSON-формат validation errors.
- Добавлены structured JSON logs с MDC-полями.
- Добавлен Micrometer Tracing + OTLP exporter для OpenTelemetry.
- Далее: ручные spans для WebSocket/outbox и collector в Docker Compose.

## Инкремент 5: service boundaries

Цель: подготовить выделение сервисов без преждевременного распила.

- Зафиксировать feature boundaries: auth, player, matchmaking, game, rating, economy, liveops, analytics.
- Вынести shared contracts в `common-events` и `common-security`.
- Начать с асинхронных consumers: analytics-service, liveops-service, retention-service.
- Оставить transactional gameplay в монолите, пока нагрузка и доменные границы не потребуют физического выделения.

## Инкремент 6: React + Vite Telegram Mini App

Цель: заменить статический Mini App на production-ready React/Vite frontend для IQ Arena.

Статус: базовая реализация готова, пройдены build и визуальный QA mobile/desktop.

- Продуктовый фокус: интеллектуальная PvP quiz arena без чужой игровой или техно-клишированной стилистики.
- Реализовать отдельный `frontend/` workspace на React + Vite.
- Сохранять build output в `src/main/resources/static/app`, чтобы backend продолжал отдавать `/app`.
- Поддержать калибровку, arena matchmaking, live duel state, профиль, рейтинг, leaderboard, сезонные награды и прогресс.
- Визуальный стиль: premium dark sport-tech/editorial UI, чистая типографика, дорогая палитра, code-native controls, оригинальная SVG/lucide iconography, плавные motion states.
- Проверять desktop/mobile, build output и визуальную верность дизайн-концепту.
