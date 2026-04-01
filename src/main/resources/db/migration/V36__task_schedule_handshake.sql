ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS scheduled_start_at timestamptz;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS scheduled_end_at timestamptz;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_status varchar(20) DEFAULT 'UNSCHEDULED';
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_proposed_by varchar(36);
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_proposed_at timestamptz;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_confirmed_by varchar(36);
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_confirmed_at timestamptz;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_reschedule_reason text;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_cancel_reason text;
ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS schedule_no_show_reason text;

UPDATE task_instances
SET schedule_status = 'UNSCHEDULED'
WHERE schedule_status IS NULL;

INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202604010000000090001', NULL, 'com.sme.onboarding.task.schedule.propose', 'Propose schedule for onboarding task', 'ACTIVE'),
('202604010000000090002', NULL, 'com.sme.onboarding.task.schedule.confirm', 'Confirm proposed schedule for onboarding task', 'ACTIVE'),
('202604010000000090003', NULL, 'com.sme.onboarding.task.schedule.reschedule', 'Reschedule onboarding task after confirmation', 'ACTIVE'),
('202604010000000090004', NULL, 'com.sme.onboarding.task.schedule.cancel', 'Cancel confirmed/proposed onboarding task schedule', 'ACTIVE'),
('202604010000000090005', NULL, 'com.sme.onboarding.task.schedule.markNoShow', 'Mark onboarding task schedule as no-show', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;

