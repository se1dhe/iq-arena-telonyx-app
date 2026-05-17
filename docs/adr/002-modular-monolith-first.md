# ADR 002: Modular Monolith First

## Status

Accepted

## Context

The MVP needs fast iteration, but the domain already has clear boundaries: auth, profile, realtime, matchmaking, game, rating, content, moderation, and analytics.

## Decision

Ship one Spring Boot deployable service with strict package boundaries. Extract services only when scale or operational pressure makes it necessary.

## Consequences

Local development and deployment stay simple. Cross-module contracts should remain explicit so future extraction is possible.

