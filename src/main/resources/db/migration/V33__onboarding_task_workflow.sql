-- Onboarding instance: optional manager / IT assignees for task generation (users.user_id)
ALTER TABLE onboarding_instances
    ADD COLUMN IF NOT EXISTS manager_user_id varchar(36);
ALTER TABLE onboarding_instances
    ADD COLUMN IF NOT EXISTS it_staff_user_id varchar(36);

-- Template: manager approval gate for generated tasks
ALTER TABLE task_templates
    ADD COLUMN IF NOT EXISTS requires_manager_approval boolean DEFAULT false;

UPDATE task_templates SET requires_manager_approval = false WHERE requires_manager_approval IS NULL;

-- Instance task row: ack, manager approval workflow
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS require_ack boolean DEFAULT false;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS acknowledged_at timestamptz;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS acknowledged_by varchar(36);
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS requires_manager_approval boolean DEFAULT false;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS approval_status varchar(30) DEFAULT 'NONE';
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS approved_by varchar(36);
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS approved_at timestamptz;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS rejection_reason text;

UPDATE task_instances SET require_ack = false WHERE require_ack IS NULL;
UPDATE task_instances SET requires_manager_approval = false WHERE requires_manager_approval IS NULL;
UPDATE task_instances SET approval_status = 'NONE' WHERE approval_status IS NULL;



