CREATE TABLE IF NOT EXISTS activity_logs (
    log_id       varchar(36) PRIMARY KEY,
    company_id   varchar(36),
    user_id      varchar(36),
    action       varchar(100),
    entity_type  varchar(50),
    entity_id    varchar(36),
    detail       text,
    created_at   timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_activity_logs_created ON activity_logs(created_at);

CREATE TABLE IF NOT EXISTS error_logs (
    error_id     varchar(36) PRIMARY KEY,
    error_code   varchar(50),
    message      text,
    stack_trace  text,
    request_id   varchar(36),
    created_at   timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_error_logs_created ON error_logs(created_at);
