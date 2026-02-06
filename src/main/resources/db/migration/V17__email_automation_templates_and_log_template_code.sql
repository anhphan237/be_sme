-- Email automation: template_code on email_logs + seed system email templates (Đợt 5)
ALTER TABLE email_logs ADD COLUMN IF NOT EXISTS template_code varchar(100);

-- System templates (company_id NULL so all tenants can use)
INSERT INTO email_templates (email_template_id, company_id, name, subject_template, body_template, status, created_at, updated_at) VALUES
('etpl_welcome_001', NULL, 'WELCOME_NEW_EMPLOYEE', 'Welcome to {{companyName}}', 'Hi {{employeeName}},\n\nWelcome to {{companyName}}. Your onboarding has started. We look forward to working with you.\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_prefirst_002', NULL, 'PRE_FIRST_DAY', 'Your first day at {{companyName}} is tomorrow', 'Hi {{employeeName}},\n\nReminder: your first day at {{companyName}} is tomorrow ({{startDate}}). See you then!\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_task_003', NULL, 'TASK_REMINDER', 'Task due soon: {{taskTitle}}', 'Hi,\n\nReminder: task "{{taskTitle}}" is due on {{dueDate}}. Please complete it in time.\n\nBest regards' , 'ACTIVE', now(), now())
ON CONFLICT (email_template_id) DO NOTHING;
