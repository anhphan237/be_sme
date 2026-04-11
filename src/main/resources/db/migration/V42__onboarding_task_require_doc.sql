ALTER TABLE task_templates
    ADD COLUMN IF NOT EXISTS require_doc boolean DEFAULT false;

ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS require_doc boolean DEFAULT false;

UPDATE task_templates
SET require_doc = false
WHERE require_doc IS NULL;

UPDATE task_instances
SET require_doc = false
WHERE require_doc IS NULL;
