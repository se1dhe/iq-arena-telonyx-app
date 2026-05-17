# ADR 006: Glicko-Like Rating

## Status

Accepted

## Context

Ranked 1v1 needs a skill rating that can later support uncertainty and provisional players.

## Decision

Store rating, RD, sigma, games played, mode, and season in `ratings`. MVP uses a simple Glicko-like update now and keeps the persistence model compatible with fuller Glicko-2.

## Consequences

Players get visible rating changes immediately after a match. The algorithm can be replaced without changing API shape.

