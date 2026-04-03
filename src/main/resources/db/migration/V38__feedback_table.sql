CREATE TABLE IF NOT EXISTS feedbacks (
    feedback_id  varchar(36) PRIMARY KEY,
    company_id   varchar(36),
    user_id      varchar(36),
    subject      varchar(255),
    content      text,
    status       varchar(30) DEFAULT 'OPEN',
    resolved_at  timestamptz,
    resolved_by  varchar(36),
    created_at   timestamptz DEFAULT now(),
    updated_at   timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_feedbacks_company ON feedbacks(company_id);
CREATE INDEX IF NOT EXISTS ix_feedbacks_status ON feedbacks(status);
