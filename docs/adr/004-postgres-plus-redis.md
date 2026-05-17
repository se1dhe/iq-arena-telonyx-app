# ADR 004: PostgreSQL Plus Redis

## Status

Accepted

## Context

Matches, answers, ratings, profiles, and question history are durable data. Matchmaking, tickets, presence, and short-lived runtime state are ephemeral.

## Decision

Use PostgreSQL for durable state and Redis for queue, presence, and temporary realtime data as the project grows. In-memory implementations are acceptable only for single-instance MVP slices.

## Consequences

The current code can stay simple while keeping a clear migration path for multiple backend instances.

