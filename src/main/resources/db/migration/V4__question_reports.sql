CREATE TABLE question_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id),
    player_id UUID NOT NULL REFERENCES players(id),
    reason VARCHAR(64) NOT NULL,
    match_id UUID,
    round_index INTEGER,
    note TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'open',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_question_reports_question_id ON question_reports(question_id);
CREATE INDEX idx_question_reports_player_id ON question_reports(player_id);
CREATE INDEX idx_question_reports_status ON question_reports(status);

