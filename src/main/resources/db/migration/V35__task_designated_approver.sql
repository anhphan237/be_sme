-- Optional per-task approver (users.user_id). When set, only this user may approve/reject
-- (instead of line manager). When null, line manager from onboarding instance / employee profile applies.
ALTER TABLE task_templates
    ADD COLUMN IF NOT EXISTS approver_user_id varchar(36);

ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS approver_user_id varchar(36);

