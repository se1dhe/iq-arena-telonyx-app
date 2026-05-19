# AGENTS.md

# AI Agents Architecture

## Overview

Проект использует набор AI-агентов для автоматизации:
- Retention
- Matchmaking
- Anti-cheat
- LiveOps
- Analytics
- PvP balancing

---

# 1. Matchmaking Agent

## Role
Подбор PvP-соперников.

## Responsibilities
- Анализ MMR
- Анализ winrate
- Баланс классов
- Prediction difficulty

## Inputs
- PvP stats
- Online count
- Class usage
- Ping

## Outputs
- Match suggestions
- Arena queue balancing

## KPIs
- Queue time
- Match fairness
- Player satisfaction

---

# 2. Retention Agent

## Role
Предотвращение ухода игроков.

## Responsibilities
- Detect churn risk
- Personalized rewards
- Return incentives
- Dynamic missions

## Inputs
- Session length
- Last login
- PvP activity
- Economy activity

## Outputs
- Bonus rewards
- Push notifications
- Dynamic events

## KPIs
- D7 retention
- D30 retention
- Returning users

---

# 3. Anti-Cheat Agent

## Role
Обнаружение читеров.

## Responsibilities
- Pattern analysis
- Bot detection
- Packet anomaly detection
- Multi-account detection

## Inputs
- Movement data
- PvP logs
- Packet timings
- Trade activity

## Outputs
- Risk scores
- Ban recommendations
- Alerts

## KPIs
- False positive rate
- Detection accuracy

---

# 4. Economy Agent

## Role
Контроль экономики сервера.

## Responsibilities
- Inflation monitoring
- Market analysis
- Reward balancing
- Sink/source balancing

## Inputs
- Trade logs
- Item creation
- Currency flow

## Outputs
- Reward modifiers
- Price alerts
- Economy reports

---

# 5. LiveOps Agent

## Role
Управление ивентами.

## Responsibilities
- Event scheduling
- Dynamic boosts
- Weekend events
- Population balancing

## Outputs
- Event rotations
- Reward scaling
- Emergency events

---

# 6. Toxicity Agent

## Role
Контроль токсичности.

## Responsibilities
- Chat analysis
- Toxicity scoring
- Spam detection

## Outputs
- Mutes
- Warnings
- Moderator alerts

---

# 7. Analytics Agent

## Role
Сбор аналитики.

## Responsibilities
- Heatmaps
- Funnel analysis
- PvP analytics
- Session analytics

## Outputs
- Dashboards
- Reports
- Recommendations

---

# Agent Communication

## Transport
- RabbitMQ
- Redis Streams

## Event Format
```json
{
  "eventType": "PLAYER_LOGIN",
  "playerId": 123,
  "timestamp": 123456789
}
```

---

# Observability

## Metrics
- Prometheus

## Logs
- Loki

## Tracing
- OpenTelemetry

---

# Security

- JWT
- IP validation
- Packet verification
- Anti-spam throttling
