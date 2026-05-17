# ADR 005: Telegram-First Auth

## Status

Accepted

## Context

The product is currently Telegram-first rather than App Store-first.

## Decision

Use Telegram Web App `initData` verification as the production login path and keep dev login for local testing. JWT identifies the player for REST calls; WebSocket uses a short-lived ticket issued from the authenticated session.

## Consequences

Telegram launch remains fast. If native iOS/macOS returns later, Apple login and account deletion flows must be added before App Review.

