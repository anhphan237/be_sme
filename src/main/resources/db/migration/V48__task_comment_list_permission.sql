INSERT INTO permissions (permission_id, company_id, code, description, status) VALUES
('202606201200000070035', NULL, 'com.sme.onboarding.task.comment.list', 'List comments on an onboarding task', 'ACTIVE')
ON CONFLICT (permission_id) DO NOTHING;
