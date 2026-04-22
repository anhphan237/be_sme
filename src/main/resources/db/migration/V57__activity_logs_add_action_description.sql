ALTER TABLE activity_logs
    ADD COLUMN IF NOT EXISTS action_description text;
