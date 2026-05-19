CREATE INDEX IF NOT EXISTS idx_ratings_leaderboard
    ON ratings(mode, season_id, rating DESC);

CREATE INDEX IF NOT EXISTS idx_questions_approved_locale_category
    ON questions(locale, category, status)
    WHERE status = 'approved';

CREATE INDEX IF NOT EXISTS idx_questions_approved_locale
    ON questions(locale, status)
    WHERE status = 'approved';

CREATE INDEX IF NOT EXISTS idx_matches_player_one_created_at
    ON matches(player_one_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_matches_player_two_created_at
    ON matches(player_two_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_matches_state_created_at
    ON matches(state, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_referral_invites_inviter_created_at
    ON referral_invites(inviter_player_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_referral_invites_invited_player_id
    ON referral_invites(invited_player_id)
    WHERE invited_player_id IS NOT NULL;
