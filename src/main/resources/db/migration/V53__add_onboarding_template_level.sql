ALTER TABLE onboarding_templates
ADD COLUMN IF NOT EXISTS level varchar(20);

UPDATE onboarding_templates
SET level = 'TENANT'
WHERE level IS NULL;

ALTER TABLE onboarding_templates
ALTER COLUMN level SET DEFAULT 'TENANT';

ALTER TABLE onboarding_templates
ALTER COLUMN level SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_onboarding_templates_level'
    ) THEN
        ALTER TABLE onboarding_templates
        ADD CONSTRAINT ck_onboarding_templates_level
        CHECK (level IN ('PLATFORM', 'TENANT'));
    END IF;
END $$;
