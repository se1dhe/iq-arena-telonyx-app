CREATE TABLE event_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(96) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error TEXT,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_event_outbox_status_next_attempt_at
    ON event_outbox(status, next_attempt_at, occurred_at);

CREATE INDEX idx_event_outbox_aggregate
    ON event_outbox(aggregate_type, aggregate_id);
