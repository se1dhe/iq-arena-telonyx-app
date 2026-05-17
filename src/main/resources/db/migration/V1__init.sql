CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE players (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    handle VARCHAR(24) NOT NULL UNIQUE,
    display_name VARCHAR(24) NOT NULL,
    avatar_id VARCHAR(64) NOT NULL DEFAULT 'avatar_01',
    locale VARCHAR(16) NOT NULL DEFAULT 'ru-RU',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players(id),
    mode VARCHAR(32) NOT NULL DEFAULT 'ranked_duel',
    season_id VARCHAR(64) NOT NULL DEFAULT 'season_0',
    rating DOUBLE PRECISION NOT NULL DEFAULT 1500,
    rd DOUBLE PRECISION NOT NULL DEFAULT 350,
    sigma DOUBLE PRECISION NOT NULL DEFAULT 0.06,
    games_played INTEGER NOT NULL DEFAULT 0,
    last_rated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(player_id, mode, season_id)
);

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    locale VARCHAR(16) NOT NULL,
    category VARCHAR(64) NOT NULL,
    difficulty VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    prompt TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_index INTEGER NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state VARCHAR(32) NOT NULL,
    player_one_id UUID NOT NULL REFERENCES players(id),
    player_two_id UUID NOT NULL REFERENCES players(id),
    winner_player_id UUID REFERENCES players(id),
    player_one_score INTEGER NOT NULL DEFAULT 0,
    player_two_score INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE TABLE match_rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id),
    round_index INTEGER NOT NULL,
    question_id UUID NOT NULL REFERENCES questions(id),
    opened_at TIMESTAMPTZ,
    deadline_at TIMESTAMPTZ,
    correct_index INTEGER NOT NULL,
    UNIQUE(match_id, round_index)
);

CREATE TABLE match_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id),
    round_index INTEGER NOT NULL,
    player_id UUID NOT NULL REFERENCES players(id),
    selected_index INTEGER,
    correct BOOLEAN NOT NULL,
    response_ms INTEGER,
    points INTEGER NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(match_id, round_index, player_id)
);
