# ADR 003: Raw WebSocket JSON Envelope

## Status

Accepted

## Context

MVP realtime needs a small command/event protocol for Telegram Web App and future clients.

## Decision

Use raw WebSocket at `/ws` with JSON messages containing `type`, `id`, `seq`, `ts`, and `payload`. Do not introduce STOMP for MVP.

## Consequences

The protocol is easy to inspect and test. The application owns routing, idempotency rules, and resync semantics.

