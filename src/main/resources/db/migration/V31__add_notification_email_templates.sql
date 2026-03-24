-- Email templates for notification types: PAYMENT_REMINDER, USAGE_ALERT, PAYMENT_FAILED, QUOTA_WARNING, ACCOUNT_SUSPENDED, TASK_ASSIGNED, ONBOARDING_STARTED, COMPANY_WELCOME, SURVEY_READY
INSERT INTO email_templates (email_template_id, company_id, name, subject_template, body_template, status, created_at, updated_at) VALUES
('etpl_pay_rem_004', NULL, 'PAYMENT_REMINDER', 'Invoice {{invoiceNo}} due on {{dueDate}}', 'Hi,\n\nReminder: invoice {{invoiceNo}} (amount {{amountTotal}} {{currency}}) is due on {{dueDate}}. Please complete payment in time.\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_usage_005', NULL, 'USAGE_ALERT', '{{alertTitle}}', 'Hi,\n\n{{alertContent}}\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_pay_fail_006', NULL, 'PAYMENT_FAILED', 'Payment retry failed', 'Hi,\n\nPayment retry failed: {{reason}}. Please check your payment method and try again.\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_quota_007', NULL, 'QUOTA_WARNING', 'Usage approaching limit', 'Hi,\n\nYour onboarding usage for {{month}} is {{usage}} of {{limit}} ({{percent}}%). {{message}}\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_suspend_008', NULL, 'ACCOUNT_SUSPENDED', 'Account suspended', 'Hi,\n\nYour account has been suspended due to repeated payment failures. Please contact support to restore access.\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_task_assign_009', NULL, 'TASK_ASSIGNED', 'New task assigned: {{taskTitle}}', 'Hi,\n\nYou have been assigned a new task: "{{taskTitle}}". Due date: {{dueDate}}.\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_onbd_start_010', NULL, 'ONBOARDING_STARTED', 'Onboarding started', 'Hi,\n\nAn onboarding has been activated. You have new tasks to complete.\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_company_011', NULL, 'COMPANY_WELCOME', 'Welcome to {{companyName}}', 'Hi,\n\nYour company {{companyName}} has been set up successfully. You can now start managing your onboarding.\n\nBest regards', 'ACTIVE', now(), now()),
('etpl_survey_012', NULL, 'SURVEY_READY', 'Survey ready', 'Hi,\n\nA survey is ready for you to complete. Please submit your response by {{dueDate}}.\n\nBest regards', 'ACTIVE', now(), now())
ON CONFLICT (email_template_id) DO NOTHING;
