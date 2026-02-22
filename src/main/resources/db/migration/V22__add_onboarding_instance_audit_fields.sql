-- Track operator who updates/completes onboarding instance
ALTER TABLE onboarding_instances
    ADD COLUMN IF NOT EXISTS updated_by varchar(36);

ALTER TABLE onboarding_instances
    ADD COLUMN IF NOT EXISTS completed_by varchar(36);
