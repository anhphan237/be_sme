ALTER TABLE checklist_templates
    ADD COLUMN IF NOT EXISTS deadline_days int;

ALTER TABLE checklist_instances
    ADD COLUMN IF NOT EXISTS open_at date;

ALTER TABLE checklist_instances
    ADD COLUMN IF NOT EXISTS deadline_at date;
