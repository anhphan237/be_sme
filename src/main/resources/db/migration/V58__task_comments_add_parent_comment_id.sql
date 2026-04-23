ALTER TABLE task_comments
    ADD COLUMN IF NOT EXISTS parent_comment_id varchar(36);

CREATE INDEX IF NOT EXISTS ix_task_comments_parent
    ON task_comments(company_id, parent_comment_id);
