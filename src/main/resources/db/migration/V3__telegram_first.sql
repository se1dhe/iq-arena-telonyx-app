ALTER TABLE players
    ADD COLUMN IF NOT EXISTS telegram_user_id BIGINT UNIQUE,
    ADD COLUMN IF NOT EXISTS telegram_username VARCHAR(64),
    ADD COLUMN IF NOT EXISTS telegram_first_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS telegram_last_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS telegram_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS referral_code VARCHAR(32) UNIQUE,
    ADD COLUMN IF NOT EXISTS referred_by_player_id UUID REFERENCES players(id);

CREATE INDEX IF NOT EXISTS idx_players_telegram_user_id ON players(telegram_user_id);
CREATE INDEX IF NOT EXISTS idx_players_referral_code ON players(referral_code);

CREATE TABLE IF NOT EXISTS bot_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players(id),
    telegram_user_id BIGINT NOT NULL UNIQUE,
    chat_id BIGINT,
    is_bot BOOLEAN NOT NULL DEFAULT false,
    language_code VARCHAR(16),
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS referral_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inviter_player_id UUID NOT NULL REFERENCES players(id),
    invited_player_id UUID REFERENCES players(id),
    referral_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'created',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
