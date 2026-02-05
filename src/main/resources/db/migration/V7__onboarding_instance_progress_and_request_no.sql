-- Add progress_percent for instance completion tracking (updated by task.updateStatus)
ALTER TABLE onboarding_instances
    ADD COLUMN IF NOT EXISTS progress_percent int DEFAULT 0;

-- Idempotency key for instance.create / instance.activate
ALTER TABLE onboarding_instances
    ADD COLUMN IF NOT EXISTS request_no varchar(255);

CREATE INDEX IF NOT EXISTS ix_onboarding_instances_request_no
    ON onboarding_instances(company_id, request_no)
    WHERE request_no IS NOT NULL;
