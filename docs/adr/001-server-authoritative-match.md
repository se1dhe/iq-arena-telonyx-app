# ADR 001: Server-Authoritative Match

## Status

Accepted

## Context

IQ Arena is a real-time ranked quiz. Client-side timers or scoring would make cheating and desync easy.

## Decision

The backend owns match state, round opening, deadlines, answer acceptance, reveal, scoring, and final result. Clients only render state and submit commands.

## Consequences

The game can support Telegram Web App now and native clients later without changing fairness rules. The backend must keep match orchestration deterministic and auditable.

